package com.sxmxdx.backend.service.comparaison;

import com.sxmxdx.backend.dto.comparaison.ComparaisonHistoriqueDTO;
import com.sxmxdx.backend.dto.comparaison.IndicateurComparaisonDTO;
import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.entity.exercice.Exercice;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceRepository;
import com.sxmxdx.backend.service.JournalAuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Compare les budgets générés de deux exercices.
 */
@Service
@Transactional(readOnly = true)
public class ComparaisonHistoriqueService {

    private static final String MODULE = "COMPARAISON";
    private static final String ENTITE = "COMPARAISON_HISTORIQUE";

    private final BudgetRepository budgets;
    private final ExerciceRepository exercices;
    private final ComparateurBudgets comparateur;
    private final JournalAuditService journalAuditService;

    public ComparaisonHistoriqueService(
            BudgetRepository budgets,
            ExerciceRepository exercices,
            ComparateurBudgets comparateur,
            JournalAuditService journalAuditService
    ) {
        this.budgets = budgets;
        this.exercices = exercices;
        this.comparateur = comparateur;
        this.journalAuditService = journalAuditService;
    }

    public List<Integer> anneesDisponibles() {
        return budgets.findAnneesAvecBudgetGenere();
    }

    public ComparaisonHistoriqueDTO comparer(Integer annee1, Integer annee2) {
        try {
            if (annee1 == null || annee2 == null || annee1.equals(annee2)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Choisir deux années différentes");
            }

            Budget budget1 = budgetGenere(annee1);
            Budget budget2 = budgetGenere(annee2);

            List<IndicateurComparaisonDTO> indicateurs = comparateur.comparer(budget1, budget2);

            ComparaisonHistoriqueDTO resultat = new ComparaisonHistoriqueDTO(
                    annee1,
                    annee2,
                    budget1.getId(),
                    budget2.getId(),
                    indicateurs,
                    comparateur.interpreter(indicateurs, annee1, annee2)
            );

            journalAuditService.enregistrer(
                    "COMPARAISON_HISTORIQUE", MODULE,
                    "Comparaison historique réussie entre " + annee1 + " et " + annee2
                            + " (budgets " + budget1.getId() + " et " + budget2.getId() + ")",
                    ENTITE, budget1.getId() + "-" + budget2.getId(), JournalAuditService.SUCCES
            );

            return resultat;

        } catch (RuntimeException e) {
            journalAuditService.enregistrer(
                    "ECHEC_COMPARAISON_HISTORIQUE", MODULE,
                    "Échec de comparaison entre " + annee1 + " et " + annee2 + " : " + e.getMessage(),
                    ENTITE, null, JournalAuditService.ECHEC
            );
            throw e;
        }
    }

    private Budget budgetGenere(Integer annee) {
        Exercice exercice = exercices.findByAnnee(annee)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Exercice absent : " + annee));

        List<Budget> candidats = budgets.findByExerciceIdAndTypeBudgetOrderByIdDesc(
                exercice.getId(), Budget.GENERE);

        if (candidats.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aucun budget pour " + annee);
        }

        if (candidats.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Plusieurs budgets pour " + annee
                            + " : sélection du budget de référence nécessaire"
            );
        }

        return candidats.get(0);
    }
}
