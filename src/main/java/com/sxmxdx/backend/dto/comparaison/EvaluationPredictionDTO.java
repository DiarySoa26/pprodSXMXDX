package com.sxmxdx.backend.dto.comparaison;

import java.util.List;

public record EvaluationPredictionDTO(
        Integer annee,
        Long budgetGenereId,
        Long budgetPrevisionnelId,
        List<IndicateurEvaluationDTO> indicateurs,
        List<String> interpretations
) {
}
