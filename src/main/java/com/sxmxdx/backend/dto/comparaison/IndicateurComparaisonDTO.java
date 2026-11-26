package com.sxmxdx.backend.dto.comparaison;

import java.math.BigDecimal;

/**
 * Évolution d'un indicateur entre deux budgets.
 * {@code evolutionPourcentage} est {@code null} si la référence est nulle.
 */
public record IndicateurComparaisonDTO(
        String code,
        String libelle,
        String unite,
        BigDecimal valeur1,
        BigDecimal valeur2,
        BigDecimal ecart,
        BigDecimal evolutionPourcentage,
        String tendance
) {
}
