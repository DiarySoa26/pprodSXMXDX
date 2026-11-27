package com.sxmxdx.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

@Service
public class ImportService {

//     private static final Path DATA_DIRECTORY = Paths.get("/data");
    private static final Path DATA_DIRECTORY = Paths.get("./data");


    private final PipelineService pipelineService;
    private final JournalAuditService journalAuditService;

    public ImportService(
            PipelineService pipelineService,
            JournalAuditService journalAuditService
    ) {
        this.pipelineService = pipelineService;
        this.journalAuditService = journalAuditService;
    }

    public Map<?, ?> importFile(
            MultipartFile file,
            Integer auteurId
    ) throws IOException {

        String safeFileName = null;

        try {
            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("Aucun fichier sélectionné.");
            }

            String originalFileName = file.getOriginalFilename();

            if (originalFileName == null || originalFileName.isBlank()) {
                throw new IllegalArgumentException("Nom du fichier invalide.");
            }

            safeFileName = Paths.get(originalFileName)
                    .getFileName()
                    .toString();

            String lowerName = safeFileName.toLowerCase();

            if (!lowerName.endsWith(".xlsx") && !lowerName.endsWith(".xlsm")) {
                throw new IllegalArgumentException(
                        "Seuls les fichiers .xlsx et .xlsm sont autorisés."
                );
            }

            Files.createDirectories(DATA_DIRECTORY);

            Path destination = DATA_DIRECTORY.resolve(safeFileName);

            Files.copy(
                    file.getInputStream(),
                    destination,
                    StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println("[IMPORT] Fichier enregistré : " + destination);

            if (!Files.exists(destination)) {
                throw new IOException(
                        "Le fichier n'a pas été enregistré dans /data."
                );
            }

            System.out.println("[IMPORT] Appel du pipeline Flask...");

            Map<?, ?> resultat = pipelineService.importerExercice(destination);

            System.out.println("[IMPORT] Réponse du pipeline : " + resultat);

            if (resultat == null) {
                throw new IllegalStateException(
                        "Le pipeline Flask n'a retourné aucun résultat."
                );
            }

            if (!Boolean.TRUE.equals(resultat.get("success"))) {
                Object message = resultat.get("message");

                throw new IllegalStateException(
                        "Échec de l'importation : "
                        + (message == null
                            ? "Le pipeline a signalé un échec."
                            : message.toString())
                );
            }

            enregistrerAudit(
                    auteurId,
                    "IMPORTATION_EXERCICE",
                    "Importation réussie du fichier : " + safeFileName,
                    "SUCCES"
            );

            return resultat;

        } catch (IOException | RuntimeException e) {

            // Détail complet uniquement dans les logs serveur
            System.err.println("[IMPORT] Erreur détaillée : " + e);
            e.printStackTrace();

            String erreurCourte = libelleErreur(e);

            enregistrerAudit(
                    auteurId,
                    "ECHEC_IMPORTATION",
                    "Échec de l'importation du fichier : "
                            + (safeFileName == null ? "Fichier inconnu" : safeFileName)
                            + " - " + erreurCourte,
                    "ECHEC"
            );

            // Le front reçoit le même libellé court
            if (e instanceof IllegalArgumentException) {
                throw new IllegalArgumentException(erreurCourte, e);
            }
            throw new IllegalStateException(erreurCourte, e);
        }
    }

    /**
     * Transforme une exception en libellé court, lisible et
     * de taille limitée, pour le journal d'audit et le front.
     */
    private static String libelleErreur(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        // Erreurs de validation : messages déjà courts et maîtrisés
        if (e instanceof IllegalArgumentException && e.getMessage() != null) {
            return tronquer(e.getMessage());
        }

        // Erreur HTTP renvoyée par Flask / l'hébergeur
        if (e instanceof org.springframework.web.client.RestClientResponseException r) {
            int status = r.getStatusCode().value();
            return switch (status) {
                case 400 -> "Fichier refusé par le pipeline (400)";
                case 404 -> "Service d'importation introuvable (404)";
                case 413 -> "Fichier trop volumineux (413)";
                case 415 -> "Format d'envoi non accepté par le pipeline (415)";
                case 500 -> "Erreur interne du pipeline (500)";
                case 502, 503, 504 -> "Pipeline indisponible (" + status + ")";
                default -> "Erreur du pipeline (HTTP " + status + ")";
            };
        }

        String message = e.getMessage() == null ? "" : e.getMessage();

        if (message.startsWith("Le pipeline Flask (") && message.contains("indisponible")) {
            return "Pipeline indisponible après plusieurs tentatives";
        }
        if (message.startsWith("Impossible de joindre le pipeline")
                || cause instanceof java.net.ConnectException
                || cause instanceof java.net.UnknownHostException) {
            return "Pipeline injoignable";
        }
        if (cause instanceof java.net.SocketTimeoutException) {
            return "Délai d'attente du pipeline dépassé";
        }
        if (message.startsWith("Échec de l'importation : ")) {
            // Message métier renvoyé par Flask (success=false)
            return tronquer(message.substring("Échec de l'importation : ".length()));
        }
        if (e instanceof IOException) {
            return "Erreur d'enregistrement du fichier";
        }

        return "Erreur inattendue pendant l'importation";
    }

    private static String tronquer(String texte) {
        String t = texte.replaceAll("\\s+", " ").trim();
        if (t.startsWith("<")) {
            return "Réponse invalide du pipeline";
        }
        return t.length() > 150 ? t.substring(0, 150) + "..." : t;
    }

    private void enregistrerAudit(
            Integer auteurId,
            String action,
            String description,
            String statut
    ) {
        try {
            journalAuditService.enregistrer(
                    auteurId,
                    action,
                    "IMPORTATION",
                    description,
                    "IMPORT_FICHIER",
                    null,
                    statut
            );
        } catch (Exception e) {
            System.err.println("[AUDIT] " + e.getMessage());
        }
    }
}