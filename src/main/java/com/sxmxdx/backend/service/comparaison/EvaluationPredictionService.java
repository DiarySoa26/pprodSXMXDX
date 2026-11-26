package com.sxmxdx.backend.service.comparaison;

import com.sxmxdx.backend.dto.comparaison.EvaluationPredictionDTO;
import com.sxmxdx.backend.dto.comparaison.IndicateurEvaluationDTO;
import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.entity.exercice.Exercice;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceRepository;
import com.sxmxdx.backend.service.JournalAuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Évalue la qualité d'une prévision en la confrontant au budget généré
 * (constaté) du même exercice.
 */
@Service
@Transactional(readOnly = true)
public class EvaluationPredictionService {

    private static final String MODULE = "COMPARAISON";
    private static final String ENTITE = "EVALUATION_PREDICTION";
    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final BudgetRepository budgets;
    private final ExerciceRepository exercices;
    private final JournalAuditService journalAuditService;

    public EvaluationPredictionService(
            BudgetRepository budgets,
            ExerciceRepository exercices,
            JournalAuditService journalAuditService
    ) {
        this.budgets = budgets;
        this.exercices = exercices;
        this.journalAuditService = journalAuditService;
    }

    public List<Integer> exercicesDisponibles() {
        return budgets.findAnneesEvaluables();
    }

    public EvaluationPredictionDTO evaluer(Integer annee) {
        try {
            if (annee == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Année obligatoire");
            }

            Exercice exercice = exercices.findByAnnee(annee)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Exercice introuvable"));

            Budget genere = budgetUnique(exercice.getId(), Budget.GENERE);
            Budget previsionnel = budgetUnique(exercice.getId(), Budget.PREVISIONNEL);

            List<IndicateurEvaluationDTO> indicateurs = Arrays.stream(IndicateurFinancier.values())
                    .map(indicateur -> evaluer(indicateur, genere, previsionnel))
                    .toList();

            EvaluationPredictionDTO resultat = new EvaluationPredictionDTO(
                    annee,
                    genere.getId(),
                    previsionnel.getId(),
                    indicateurs,
                    indicateurs.stream().map(EvaluationPredictionService::interpreter).toList()
            );

            journalAuditService.enregistrer(
                    "EVALUATION_PREDICTION", MODULE,
                    "Évaluation de la prévision pour l'exercice " + annee
                            + " (budget généré " + genere.getId()
                            + ", budget prévisionnel " + previsionnel.getId() + ")",
                    ENTITE, String.valueOf(exercice.getId()), JournalAuditService.SUCCES
            );

            return resultat;

        } catch (RuntimeException e) {
            journalAuditService.enregistrer(
                    "ECHEC_EVALUATION_PREDICTION", MODULE,
                    "Échec de l'évaluation pour l'exercice " + annee + " : " + e.getMessage(),
                    ENTITE, annee == null ? null : String.valueOf(annee), JournalAuditService.ECHEC
            );
            throw e;
        }
    }

    private Budget budgetUnique(Long exerciceId, String type) {
        List<Budget> candidats = budgets.findByExerciceIdAndTypeBudgetOrderByIdDesc(exerciceId, type);

        if (candidats.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Évaluation indisponible : budget " + type + " absent pour cet exercice"
            );
        }

        if (candidats.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Plusieurs budgets " + type
                            + " existent : choisir une version de référence avant l'évaluation"
            );
        }

        return candidats.get(0);
    }

    private static IndicateurEvaluationDTO evaluer(
            IndicateurFinancier indicateur,
            Budget genere,
            Budget previsionnel
    ) {
        BigDecimal reel = indicateur.valeur(genere);
        BigDecimal prevu = indicateur.valeur(previsionnel);

        if (reel == null || prevu == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Valeur absente pour l'indicateur " + indicateur.name()
            );
        }

        BigDecimal ecart = prevu.subtract(reel);
        BigDecimal erreurAbsolue = ecart.abs();
        BigDecimal erreurPourcentage = reel.signum() == 0
                ? null
                : erreurAbsolue.multiply(CENT).divide(reel.abs(), 2, RoundingMode.HALF_UP);

        String diagnostic = switch (ecart.signum()) {
            case 1 -> "SURESTIMATION";
            case -1 -> "SOUS_ESTIMATION";
            default -> "CONFORME";
        };

        return new IndicateurEvaluationDTO(
                indicateur.name(),
                indicateur.libelle(),
                indicateur.unite(),
                reel,
                prevu,
                ecart,
                erreurAbsolue,
                erreurPourcentage,
                diagnostic
        );
    }

    private static String interpreter(IndicateurEvaluationDTO indicateur) {
        String erreurRelative = indicateur.erreurPourcentage() == null
                ? "erreur relative non calculable (valeur constatée nulle)"
                : "erreur relative absolue de " + indicateur.erreurPourcentage().toPlainString() + " %";

        if (IndicateurFinancier.UNITE_POINT.equals(indicateur.unite())) {
            BigDecimal points = indicateur.ecart().multiply(CENT).setScale(2, RoundingMode.HALF_UP);
            return indicateur.libelle() + " : écart signé de " + points.toPlainString()
                    + " point(s) ; " + erreurRelative + ".";
        }

        String diagnostic = indicateur.diagnostic().toLowerCase(Locale.ROOT).replace('_', ' ');
        return indicateur.libelle() + " : " + diagnostic + " ; " + erreurRelative + ".";
    }
}
