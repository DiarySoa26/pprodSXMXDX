package com.sxmxdx.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private static final Set<String> EXTENSIONS_AUTORISEES = Set.of(".xlsx", ".xlsm");
    private static final String MODULE = "IMPORTATION";
    private static final String ENTITE = "IMPORT_FICHIER";

    private final Path dossierImport;
    private final PipelineService pipelineService;
    private final JournalAuditService journalAuditService;

    public ImportService(
            @Value("${app.import.directory}") String dossierImport,
            PipelineService pipelineService,
            JournalAuditService journalAuditService
    ) {
        this.dossierImport = Paths.get(dossierImport).toAbsolutePath().normalize();
        this.pipelineService = pipelineService;
        this.journalAuditService = journalAuditService;
    }

    public Map<?, ?> importFile(MultipartFile fichier) throws IOException {
        String nomFichier = null;

        try {
            nomFichier = nomFichierValide(fichier);
            enregistrerSurDisque(fichier, nomFichier);

            Map<?, ?> resultat = pipelineService.importerExercice(nomFichier);
            log.info("Réponse du pipeline pour {} : {}", nomFichier, resultat);

            if (!PipelineService.estSucces(resultat)) {
                Object message = resultat == null ? null : resultat.get("message");
                throw new IllegalStateException(
                        "Échec de l'importation : "
                                + (message == null ? "le pipeline a signalé un échec." : message)
                );
            }

            journalAuditService.enregistrer(
                    "IMPORTATION_EXERCICE", MODULE,
                    "Importation réussie du fichier : " + nomFichier,
                    ENTITE, null, JournalAuditService.SUCCES
            );

            return resultat;

        } catch (IOException | RuntimeException e) {
            journalAuditService.enregistrer(
                    "ECHEC_IMPORTATION", MODULE,
                    "Échec de l'importation du fichier : "
                            + (nomFichier == null ? "fichier inconnu" : nomFichier)
                            + " - " + e.getMessage(),
                    ENTITE, null, JournalAuditService.ECHEC
            );
            throw e;
        }
    }

    /**
     * Ne conserve que le nom du fichier (aucun chemin) et vérifie son extension.
     */
    private static String nomFichierValide(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier sélectionné.");
        }

        String nomOriginal = fichier.getOriginalFilename();

        if (nomOriginal == null || nomOriginal.isBlank()) {
            throw new IllegalArgumentException("Nom du fichier invalide.");
        }

        // Neutralise les chemins Windows et Unix (« ..\ », « ../ »)
        String nom = Paths.get(nomOriginal.replace('\\', '/')).getFileName().toString();
        String nomMinuscule = nom.toLowerCase(Locale.ROOT);

        if (EXTENSIONS_AUTORISEES.stream().noneMatch(nomMinuscule::endsWith)) {
            throw new IllegalArgumentException("Seuls les fichiers .xlsx et .xlsm sont autorisés.");
        }

        return nom;
    }

    private void enregistrerSurDisque(MultipartFile fichier, String nomFichier) throws IOException {
        Files.createDirectories(dossierImport);

        Path destination = dossierImport.resolve(nomFichier).normalize();

        if (!destination.startsWith(dossierImport)) {
            throw new IllegalArgumentException("Nom du fichier invalide.");
        }

        try (InputStream contenu = fichier.getInputStream()) {
            Files.copy(contenu, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("Fichier enregistré : {}", destination);
    }
}
