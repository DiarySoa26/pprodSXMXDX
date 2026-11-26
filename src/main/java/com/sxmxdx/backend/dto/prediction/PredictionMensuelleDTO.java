package com.sxmxdx.backend.dto.prediction;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;

/**
 * Ligne mensuelle d'une prédiction. {@code mois} est le libellé du mois.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PredictionMensuelleDTO(
        Integer moisId,
        String mois,
        BigDecimal ventes,
        BigDecimal production,
        BigDecimal caExport,
        BigDecimal chargesExploitation,
        BigDecimal fraisExport,
        BigDecimal masseSalariale,
        BigDecimal resultatOperationnel,
        BigDecimal margeOperationnelle
) {
}
