package com.sxmxdx.backend.dto;

/**
 * Réponse de connexion. En cas de succès, {@code token} est le jeton à
 * transmettre dans l'en-tête {@code Authorization: Bearer <token>}.
 */
public record LoginResponse(
        boolean success,
        String message,
        Integer id,
        String name,
        String email,
        String role,
        String token,
        Long expiresIn
) {

    public static LoginResponse echec(String message) {
        return new LoginResponse(false, message, null, null, null, null, null, null);
    }
}
