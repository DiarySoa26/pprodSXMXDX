package com.sxmxdx.backend.dto.prediction;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;

/**
 * Entrée de l'historique des prédictions.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PredictionResumeDTO(
        Long id,
        Integer annee,
        Integer anneeReference,
        String statut,
        String modele,
        LocalDateTime dateGeneration
) {
}
