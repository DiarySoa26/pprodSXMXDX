package com.sxmxdx.backend.dto.campagne;

import java.time.LocalDate;

/**
 * Création d'une campagne a posteriori, une fois le budget de l'exercice
 * déjà importé et généré.
 */
public record CampagneApresBudgetRequest(
        Long exerciceId,
        Integer anneeExercice,
        LocalDate dateVerification,
        LocalDate dateValidation
) {
}
