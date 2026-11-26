package com.sxmxdx.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/**
 * Traduit les exceptions en réponses JSON {@code {success, message}}.
 * Les erreurs inattendues sont journalisées, mais leur détail technique
 * n'est jamais renvoyé au client.
 */
@RestControllerAdvice
public class GestionnaireErreurs {

    private static final Logger log = LoggerFactory.getLogger(GestionnaireErreurs.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> statut(ResponseStatusException e) {
        return reponse(e.getStatusCode(), e.getReason());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentInvalide(IllegalArgumentException e) {
        return reponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> corpsIllisible(HttpMessageNotReadableException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps de requête invalide.");
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> pipelineIndisponible(RestClientException e) {
        log.error("Erreur lors de l'appel au pipeline", e);
        return reponse(
                HttpStatus.BAD_GATEWAY,
                "Le pipeline de traitement est indisponible ou a renvoyé une erreur."
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> autre(Exception e) {
        // Exceptions Spring MVC (404, 405, paramètre manquant...) : statut conservé
        if (e instanceof ErrorResponse erreurSpring) {
            HttpStatusCode statut = erreurSpring.getStatusCode();
            return reponse(statut, statut.is4xxClientError() ? "Requête invalide." : null);
        }

        log.error("Erreur inattendue", e);
        return reponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur interne est survenue."
        );
    }

    private static ResponseEntity<Map<String, Object>> reponse(
            HttpStatusCode statut,
            String message
    ) {
        String texte = message == null || message.isBlank()
                ? "Erreur " + statut.value()
                : message;

        return ResponseEntity
                .status(statut)
                .body(Map.of("success", false, "message", texte));
    }
}
