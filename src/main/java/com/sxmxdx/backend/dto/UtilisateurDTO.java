package com.sxmxdx.backend.dto;

import com.sxmxdx.backend.entity.Utilisateur;

public record UtilisateurDTO(
        Integer id,
        String nom,
        String prenom,
        String email,
        String role,
        boolean actif
) {

    public static UtilisateurDTO depuis(Utilisateur utilisateur) {
        return new UtilisateurDTO(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getRole().getNom(),
                Boolean.TRUE.equals(utilisateur.getActif())
        );
    }
}
