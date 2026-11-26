package com.sxmxdx.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Échec d'une tentative de connexion, avec le statut HTTP à renvoyer.
 */
public class EchecConnexionException extends RuntimeException {

    private final HttpStatus statut;

    public EchecConnexionException(HttpStatus statut, String message) {
        super(message);
        this.statut = statut;
    }

    public HttpStatus getStatut() {
        return statut;
    }
}
