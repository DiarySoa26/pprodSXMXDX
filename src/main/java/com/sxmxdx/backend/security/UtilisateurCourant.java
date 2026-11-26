package com.sxmxdx.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

/**
 * Accès à l'utilisateur authentifié de la requête en cours.
 * <p>
 * L'auteur d'une action est toujours déduit du jeton, jamais d'un paramètre
 * envoyé par le client (qui serait falsifiable).
 */
public final class UtilisateurCourant {

    private UtilisateurCourant() {
    }

    public static Optional<Integer> id() {
        Authentication authentification =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentification instanceof JwtAuthenticationToken jeton) {
            return Optional.of(Integer.valueOf(jeton.getName()));
        }

        return Optional.empty();
    }
}
