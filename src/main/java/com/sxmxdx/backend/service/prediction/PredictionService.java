package com.sxmxdx.backend.service.prediction;

import com.sxmxdx.backend.dto.prediction.PredictionDTO;
import com.sxmxdx.backend.dto.prediction.PredictionResumeDTO;
import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.entity.exercice.Exercice;
import com.sxmxdx.backend.entity.prediction.PredictionAnalyse;
import com.sxmxdx.backend.repository.budget.BudgetMensuelRepository;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceRepository;
import com.sxmxdx.backend.repository.prediction.PredictionAnalyseRepository;
import com.sxmxdx.backend.service.JournalAuditService;
import com.sxmxdx.backend.service.PipelineService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PredictionService {

    private static final String MODULE = "PREDICTION";

    private final PipelineService pipelineService;
    private final BudgetRepository budgets;
    private final BudgetMensuelRepository budgetsMensuels;
    private final ExerciceRepository exercices;
    private final PredictionAnalyseRepository analyses;
    private final JournalAuditService journalAuditService;

    public PredictionService(
            PipelineService pipelineService,
            BudgetRepository budgets,
            BudgetMensuelRepository budgetsMensuels,
            ExerciceRepository exercices,
            PredictionAnalyseRepository analyses,
            JournalAuditService journalAuditService
    ) {
        this.pipelineService = pipelineService;
        this.budgets = budgets;
        this.budgetsMensuels = budgetsMensuels;
        this.exercices = exercices;
        this.analyses = analyses;
        this.journalAuditService = journalAuditService;
    }

    public Map<?, ?> genererPrediction() {
        try {
            Map<?, ?> resultat = pipelineService.genererPrediction();
            boolean succes = PipelineService.estSucces(resultat);

            auditer(
                    succes ? "GENERATION_PREDICTION" : "ECHEC_GENERATION_PREDICTION",
                    succes ? "Génération des prédictions réussie." : "Échec de génération des prédictions.",
                    succes ? JournalAuditService.SUCCES : JournalAuditService.ECHEC
            );

            return resultat;

        } catch (RuntimeException e) {
            auditer("ECHEC_GENERATION_PREDICTION", "Erreur : " + e.getMessage(), JournalAuditService.ECHEC);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Optional<PredictionDTO> dernierePrediction() {
        return budgets.findFirstByTypeBudgetOrderByIdDesc(Budget.PREVISIONNEL)
                .flatMap(this::detailler);
    }

    @Transactional(readOnly = true)
    public List<PredictionResumeDTO> listerPredictions() {
        return budgets.findResumesPredictions();
    }

    @Transactional(readOnly = true)
    public Optional<PredictionDTO> rechercherPrediction(Long id) {
        return budgets.findByIdAndTypeBudget(id, Budget.PREVISIONNEL)
                .flatMap(this::detailler);
    }

    /** Une prédiction sans exercice rattaché n'est pas exposée. */
    private Optional<PredictionDTO> detailler(Budget budget) {
        return exercices.findById(budget.getExerciceId())
                .map(Exercice::getAnnee)
                .map(annee -> PredictionDTO.depuis(
                        budget,
                        annee,
                        analyses.findById(budget.getId())
                                .map(PredictionAnalyse::getResume)
                                .orElse(null),
                        budgetsMensuels.findDetailMensuel(budget.getId())
                ));
    }

    private void auditer(String action, String description, String statut) {
        journalAuditService.enregistrer(action, MODULE, description, MODULE, null, statut);
    }
}
