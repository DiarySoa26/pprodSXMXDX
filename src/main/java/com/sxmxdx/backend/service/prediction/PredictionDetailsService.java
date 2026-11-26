package com.sxmxdx.backend.service.prediction;

import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.entity.budget.BudgetMensuel;
import com.sxmxdx.backend.entity.prediction.PredictionChargeCategorie;
import com.sxmxdx.backend.repository.budget.BudgetMensuelRepository;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceRepository;
import com.sxmxdx.backend.repository.prediction.PredictionChargeCategorieRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;

/**
 * Détails d'un budget prévisionnel (amortissements, charges par catégorie)
 * pour une année donnée.
 */
@Service
@Transactional(readOnly = true)
public class PredictionDetailsService {

    private static final String[] MOIS = {
            "Janvier", "Février", "Mars", "Avril",
            "Mai", "Juin", "Juillet", "Août",
            "Septembre", "Octobre", "Novembre", "Décembre"
    };

    private final BudgetRepository budgets;
    private final ExerciceRepository exercices;
    private final BudgetMensuelRepository budgetsMensuels;
    private final PredictionChargeCategorieRepository charges;

    public PredictionDetailsService(
            BudgetRepository budgets,
            ExerciceRepository exercices,
            BudgetMensuelRepository budgetsMensuels,
            PredictionChargeCategorieRepository charges
    ) {
        this.budgets = budgets;
        this.exercices = exercices;
        this.budgetsMensuels = budgetsMensuels;
        this.charges = charges;
    }

    public Map<String, Object> getAmortissements(Integer annee) {
        Budget budget = dernierBudgetPrevisionnel(annee);

        Map<Integer, BigDecimal> parMois = new HashMap<>();
        for (BudgetMensuel ligne : budgetsMensuels.findByBudgetIdOrderByMoisIdAsc(budget.getId())) {
            parMois.merge(ligne.getMoisId(), montant(ligne.getAmortissements()), BigDecimal::add);
        }

        List<Map<String, Object>> mensuel = new ArrayList<>();
        for (int mois = 1; mois <= 12; mois++) {
            Map<String, Object> item = ligneMois(mois);
            item.put("montant", parMois.getOrDefault(mois, BigDecimal.ZERO));
            mensuel.add(item);
        }

        Map<String, Object> reponse = entete(annee, budget, total(parMois.values()));
        reponse.put("mensuel", mensuel);
        return reponse;
    }

    public Map<String, Object> getChargesCategories(Integer annee) {
        Budget budget = dernierBudgetPrevisionnel(annee);

        Map<String, BigDecimal> totauxCategories = new TreeMap<>();
        Map<Integer, BigDecimal> totauxMois = new TreeMap<>();
        List<Map<String, Object>> details = new ArrayList<>();

        for (PredictionChargeCategorie ligne
                : charges.findByBudgetIdOrderByMoisIdAscCategorieAsc(budget.getId())) {
            BigDecimal valeur = montant(ligne.getMontantPrevu());

            totauxCategories.merge(ligne.getCategorie(), valeur, BigDecimal::add);
            totauxMois.merge(ligne.getMoisId(), valeur, BigDecimal::add);

            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("mois_id", ligne.getMoisId());
            detail.put("categorie", ligne.getCategorie());
            detail.put("montant_prevu", valeur);
            details.add(detail);
        }

        List<Map<String, Object>> categories = new ArrayList<>();
        totauxCategories.forEach((categorie, totalAnnuel) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("categorie", categorie);
            item.put("total_annuel", totalAnnuel);
            categories.add(item);
        });

        List<Map<String, Object>> mensuel = new ArrayList<>();
        for (int mois = 1; mois <= 12; mois++) {
            Map<String, Object> item = ligneMois(mois);
            item.put("total", totauxMois.getOrDefault(mois, BigDecimal.ZERO));
            mensuel.add(item);
        }

        Map<String, Object> reponse = entete(annee, budget, total(totauxMois.values()));
        reponse.put("categories", categories);
        reponse.put("mensuel", mensuel);
        reponse.put("details", details);
        return reponse;
    }

    private Budget dernierBudgetPrevisionnel(Integer annee) {
        return exercices.findByAnnee(annee)
                .flatMap(exercice -> budgets.findFirstByExerciceIdAndTypeBudgetOrderByIdDesc(
                        exercice.getId(), Budget.PREVISIONNEL))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Aucun budget prévisionnel pour " + annee));
    }

    private static Map<String, Object> entete(Integer annee, Budget budget, BigDecimal totalAnnuel) {
        Map<String, Object> reponse = new LinkedHashMap<>();
        reponse.put("annee", annee);
        reponse.put("budget_id", budget.getId());
        reponse.put("type_budget", Budget.PREVISIONNEL);
        reponse.put("total_annuel", totalAnnuel);
        return reponse;
    }

    private static Map<String, Object> ligneMois(int mois) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("mois_id", mois);
        item.put("mois", MOIS[mois - 1]);
        return item;
    }

    private static BigDecimal total(Collection<BigDecimal> valeurs) {
        return valeurs.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal montant(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
