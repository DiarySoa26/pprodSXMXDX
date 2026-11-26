package com.sxmxdx.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Client HTTP du pipeline de traitement (Flask / Spark).
 */
@Service
public class PipelineService {

    private static final Logger log = LoggerFactory.getLogger(PipelineService.class);

    private final RestClient restClient;
    private final String dossierDonnees;

    public PipelineService(
            @Value("${pipeline.api.url}") String pipelineApiUrl,
            @Value("${pipeline.data.directory}") String dossierDonnees
    ) {
        log.info("URL du pipeline : {}", pipelineApiUrl);

        this.restClient = RestClient.builder().baseUrl(pipelineApiUrl).build();
        this.dossierDonnees = dossierDonnees.endsWith("/")
                ? dossierDonnees
                : dossierDonnees + "/";
    }

    public Map<?, ?> importerExercice(String nomFichier) {
        String cheminFichier = dossierDonnees + nomFichier;
        log.info("Import transmis au pipeline : {}", cheminFichier);

        return restClient.post()
                .uri("/import")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("filePath", cheminFichier))
                .retrieve()
                .body(Map.class);
    }

    public Map<?, ?> genererBudget() {
        log.info("Demande de génération du budget");
        return exiger(
                restClient.post().uri("/api/budgets/generer").retrieve().body(Map.class),
                "Aucune réponse du pipeline lors de la génération du budget."
        );
    }

    public Map<?, ?> genererPrediction() {
        log.info("Demande de génération de la prédiction");
        return exiger(
                restClient.post().uri("/api/predictions/generer").retrieve().body(Map.class),
                "Aucune réponse du pipeline lors de la génération de la prédiction."
        );
    }

    public ResponseEntity<byte[]> telechargerFichier(Integer importId) {
        log.info("Téléchargement du fichier de l'import {}", importId);

        return restClient.get()
                .uri("/api/imports/{importId}/fichier", importId)
                .retrieve()
                .toEntity(byte[].class);
    }

    private static Map<?, ?> exiger(Map<?, ?> reponse, String messageSiVide) {
        if (reponse == null) {
            throw new IllegalStateException(messageSiVide);
        }
        return reponse;
    }

    /** Vrai si la réponse du pipeline signale un succès ({@code "success": true}). */
    public static boolean estSucces(Map<?, ?> reponse) {
        return reponse != null && Boolean.TRUE.equals(reponse.get("success"));
    }
}
