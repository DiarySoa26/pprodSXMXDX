package com.sxmxdx.backend.dto.comparaison;

import java.math.BigDecimal;

/**
 * Écart entre la valeur constatée (budget généré) et la valeur prévue.
 * {@code diagnostic} vaut SURESTIMATION, SOUS_ESTIMATION ou CONFORME.
 */
public record IndicateurEvaluationDTO(
        String code,
        String libelle,
        String unite,
        BigDecimal constate,
        BigDecimal prevision,
        BigDecimal ecart,
        BigDecimal erreurAbsolue,
        BigDecimal erreurPourcentage,
        String diagnostic
) {
}
