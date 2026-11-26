package com.sxmxdx.backend.dto.prediction;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.sxmxdx.backend.entity.budget.Budget;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Prédiction (budget prévisionnel) détaillée, avec sa ventilation mensuelle.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PredictionDTO(
        Long id,
        Integer annee,
        Integer anneeReference,
        String typeBudget,
        String statut,
        String modele,
        String versionModele,
        BigDecimal caExport,
        BigDecimal chargesExploitation,
        BigDecimal fraisExport,
        BigDecimal masseSalariale,
        BigDecimal resultatOperationnel,
        BigDecimal margeOperationnelle,
        LocalDateTime dateGeneration,
        String resume,
        List<PredictionMensuelleDTO> mois
) {

    public static PredictionDTO depuis(
            Budget budget,
            Integer annee,
            String resume,
            List<PredictionMensuelleDTO> mois
    ) {
        return new PredictionDTO(
                budget.getId(),
                annee,
                budget.getAnneeReference(),
                budget.getTypeBudget(),
                budget.getStatut(),
                budget.getModele(),
                budget.getVersionModele(),
                budget.getCaExport(),
                budget.getChargesExploitation(),
                budget.getFraisExport(),
                budget.getMasseSalariale(),
                budget.getResultatOperationnel(),
                budget.getMargeOperationnelle(),
                budget.getDateGeneration(),
                resume,
                mois
        );
    }
}
