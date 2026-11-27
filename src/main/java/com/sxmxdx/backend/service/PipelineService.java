package com.sxmxdx.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

@Service
public class PipelineService {

    private final RestClient restClient;
    private final String baseUrl;
    private final int maxTentatives;
    private final long delaiEntreTentativesMs;

    public PipelineService(
            @Value("${pipeline.api.url}") String pipelineApiUrl,
            @Value("${pipeline.api.connect-timeout:30s}") Duration connectTimeout,
            @Value("${pipeline.api.read-timeout:10m}") Duration readTimeout,
            @Value("${pipeline.api.max-retries:3}") int maxTentatives,
            @Value("${pipeline.api.retry-delay:15s}") Duration delaiEntreTentatives
    ) {
        this.baseUrl = normaliserUrl(pipelineApiUrl);
        this.maxTentatives = Math.max(1, maxTentatives);
        this.delaiEntreTentativesMs = delaiEntreTentatives.toMillis();

        System.out.println("[PIPELINE] URL Flask : " + baseUrl);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    /**
     * Envoie le fichier à Flask en multipart/form-data (champ "file").
     * Indispensable quand Flask est hébergé séparément (Render) :
     * il n'a pas accès au disque du backend.
     */
    public Map<?, ?> importerExercice(Path fichier) {
        if (!Files.exists(fichier)) {
            throw new IllegalStateException(
                    "Fichier introuvable avant l'envoi au pipeline : " + fichier
            );
        }

        String fileName = fichier.getFileName().toString();

        System.out.println("[PIPELINE] Envoi du fichier à Flask : " + fileName);

        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new FileSystemResource(fichier));
        form.add("fileName", fileName);

        return appeler("import", () -> restClient.post()
                .uri("/import")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .body(Map.class));
    }

    public Map<?, ?> genererBudget() {
        System.out.println("[PIPELINE] Demande de génération du budget...");

        Map<?, ?> resultat = appeler("génération budget", () -> restClient.post()
                .uri("/api/budgets/generer")
                .retrieve()
                .body(Map.class));

        if (resultat == null) {
            throw new RuntimeException(
                    "Aucune réponse du pipeline Flask."
            );
        }

        System.out.println(
                "[PIPELINE] Réponse génération budget : " + resultat
        );

        return resultat;
    }

    public Map<?, ?> genererPrediction() {
        System.out.println(
                "[PIPELINE] Demande de génération de la prédiction..."
        );

        Map<?, ?> resultat = appeler("génération prédiction", () -> restClient.post()
                .uri("/api/predictions/generer")
                .retrieve()
                .body(Map.class));

        if (resultat == null) {
            throw new RuntimeException(
                    "Aucune réponse du pipeline de prédiction."
            );
        }

        System.out.println("[PIPELINE] Réponse prédiction reçue.");

        return resultat;
    }

    public ResponseEntity<byte[]> telechargerFichier(Integer importId) {
        System.out.println(
                "[PIPELINE] Téléchargement du fichier import : " + importId
        );

        return appeler("téléchargement fichier", () -> restClient.get()
                .uri("/api/imports/{importId}/fichier", importId)
                .retrieve()
                .toEntity(byte[].class));
    }

    /**
     * Exécute l'appel en réessayant sur 502/503/504 et erreurs réseau
     * (cas typique : service Render endormi qui redémarre),
     * et remplace la page HTML d'erreur par un message lisible.
     */
    private <T> T appeler(String operation, Supplier<T> appel) {
        RuntimeException derniereErreur = null;

        for (int tentative = 1; tentative <= maxTentatives; tentative++) {
            try {
                return appel.get();

            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean reessayable = status == 502 || status == 503 || status == 504;

                if (!reessayable) {
                    System.err.println(
                            "[PIPELINE] Flask a répondu " + status
                                    + " (" + operation + ") : "
                                    + extraireMessage(e.getResponseBodyAsString())
                    );
                    // Exception d'origine conservée : les contrôleurs
                    // s'appuient sur son code HTTP (404, 400...).
                    throw e;
                }

                derniereErreur = new IllegalStateException(
                        "Le pipeline Flask (" + baseUrl + ") est indisponible : HTTP "
                                + status + " après " + maxTentatives
                                + " tentative(s). Vérifiez que le service est démarré"
                                + " et consultez ses logs.",
                        e
                );

            } catch (ResourceAccessException e) {
                derniereErreur = new IllegalStateException(
                        "Impossible de joindre le pipeline Flask (" + baseUrl + ") : "
                                + e.getMostSpecificCause().getMessage(),
                        e
                );
            }

            System.out.println(
                    "[PIPELINE] Échec " + operation + " (tentative "
                            + tentative + "/" + maxTentatives + ") : "
                            + derniereErreur.getMessage()
            );

            if (tentative < maxTentatives) {
                attendre();
            }
        }

        throw derniereErreur;
    }

    private void attendre() {
        try {
            Thread.sleep(delaiEntreTentativesMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Appel au pipeline interrompu.", ie);
        }
    }

    private static String extraireMessage(String corps) {
        if (corps == null || corps.isBlank()) {
            return "(réponse vide)";
        }
        if (corps.trim().startsWith("<")) {
            return "(page HTML d'erreur renvoyée par l'hébergeur)";
        }
        return corps.length() > 500 ? corps.substring(0, 500) + "..." : corps;
    }

    private static String normaliserUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException(
                    "pipeline.api.url n'est pas configuré."
            );
        }
        String u = url.trim();
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        while (u.endsWith("/")) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }
}
