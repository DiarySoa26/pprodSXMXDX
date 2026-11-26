package com.sxmxdx.backend.controller;

import com.sxmxdx.backend.service.ImportService;
import com.sxmxdx.backend.service.PipelineService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private static final Logger log = LoggerFactory.getLogger(ImportController.class);

    private final ImportService importService;
    private final PipelineService pipelineService;

    public ImportController(ImportService importService, PipelineService pipelineService) {
        this.importService = importService;
        this.pipelineService = pipelineService;
    }

    @PostMapping
    public Map<?, ?> importerExercice(@RequestParam("file") MultipartFile file) throws IOException {
        return importService.importFile(file);
    }

    @GetMapping("/{importId}/fichier")
    public ResponseEntity<byte[]> telechargerFichier(@PathVariable Integer importId) {
        if (importId == null || importId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant d'import invalide.");
        }

        ResponseEntity<byte[]> reponsePipeline;

        try {
            reponsePipeline = pipelineService.telechargerFichier(importId);
        } catch (RestClientResponseException e) {
            log.warn("Téléchargement de l'import {} refusé par le pipeline : {}",
                    importId, e.getResponseBodyAsString());

            String message = switch (e.getStatusCode().value()) {
                case 404 -> "Import ou fichier Excel introuvable.";
                case 400 -> "Demande de téléchargement invalide.";
                default -> "Erreur du pipeline pendant le téléchargement.";
            };

            throw new ResponseStatusException(e.getStatusCode(), message);
        }

        byte[] contenu = reponsePipeline.getBody();

        if (contenu == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY, "Le pipeline a retourné un fichier vide."
            );
        }

        HttpHeaders entetes = new HttpHeaders();
        MediaType typeContenu = reponsePipeline.getHeaders().getContentType();
        entetes.setContentType(typeContenu != null ? typeContenu : MediaType.APPLICATION_OCTET_STREAM);
        entetes.setContentLength(contenu.length);

        String disposition = reponsePipeline.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (disposition != null) {
            entetes.set(HttpHeaders.CONTENT_DISPOSITION, disposition);
        }

        return new ResponseEntity<>(contenu, entetes, reponsePipeline.getStatusCode());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> echecImport(IllegalStateException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", e.getMessage()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<Map<String, Object>> erreurFichier(IOException e) {
        log.error("Erreur d'écriture du fichier importé", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Impossible d'enregistrer le fichier importé."));
    }
}
