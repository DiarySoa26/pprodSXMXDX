package com.sxmxdx.backend.dto;

/**
 * Création ou modification d'un utilisateur.
 * En modification, un mot de passe vide conserve le mot de passe actuel.
 */
public record UtilisateurRequest(
        String nom,
        String prenom,
        String email,
        String motDePasse,
        String role
) {
}
