package com.sxmxdx.backend.dto.comparaison;

import java.util.List;

public record ComparaisonProspectiveDTO(
        Integer anneeBudget,
        Integer anneePrevision,
        Long budgetId,
        Long previsionId,
        List<IndicateurComparaisonDTO> indicateurs,
        List<String> interpretations
) {
}
