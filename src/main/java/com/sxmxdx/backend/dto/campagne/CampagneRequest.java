package com.sxmxdx.backend.dto.campagne;

import java.time.LocalDate;

/**
 * Création ou modification d'une campagne budgétaire et de ses échéances.
 */
public record CampagneRequest(
        String nom,
        Long exerciceId,
        LocalDate dateDebut,
        LocalDate dateFin,
        LocalDate dateImportation,
        LocalDate dateGeneration,
        LocalDate dateVerification,
        LocalDate dateValidation
) {
}
