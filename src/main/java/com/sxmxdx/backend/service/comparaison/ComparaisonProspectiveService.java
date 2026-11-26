package com.sxmxdx.backend.service.comparaison;

import com.sxmxdx.backend.dto.comparaison.ComparaisonProspectiveDTO;
import com.sxmxdx.backend.dto.comparaison.IndicateurComparaisonDTO;
import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.service.JournalAuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Compare un budget généré à une prévision d'une année ultérieure.
 */
@Service
@Transactional(readOnly = true)
public class ComparaisonProspectiveService {

    private static final String MODULE = "COMPARAISON";
    private static final String ENTITE = "COMPARAISON_PROSPECTIVE";

    private final BudgetRepository budgets;
    private final ComparateurBudgets comparateur;
    private final JournalAuditService journalAuditService;

    public ComparaisonProspectiveService(
            BudgetRepository budgets,
            ComparateurBudgets comparateur,
            JournalAuditService journalAuditService
    ) {
        this.budgets = budgets;
        this.comparateur = comparateur;
        this.journalAuditService = journalAuditService;
    }

    public List<Integer> getBudgetsDisponibles() {
        return budgets.findAnneesAbouties(Budget.GENERE);
    }

    public List<Integer> getPrevisionsDisponibles() {
        return budgets.findAnneesAbouties(Budget.PREVISIONNEL);
    }

    public ComparaisonProspectiveDTO comparer(Integer anneeBudget, Integer anneePrevision) {
        try {
            if (anneeBudget == null || anneePrevision == null || anneePrevision <= anneeBudget) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Les années de comparaison sont invalides");
            }

            Budget budget = budgetUnique(anneeBudget, Budget.GENERE);
            Budget prevision = budgetUnique(anneePrevision, Budget.PREVISIONNEL);

            List<IndicateurComparaisonDTO> indicateurs = comparateur.comparer(budget, prevision);

            ComparaisonProspectiveDTO resultat = new ComparaisonProspectiveDTO(
                    anneeBudget,
                    anneePrevision,
                    budget.getId(),
                    prevision.getId(),
                    indicateurs,
                    comparateur.interpreter(indicateurs, anneeBudget, anneePrevision)
            );

            journalAuditService.enregistrer(
                    "COMPARAISON_PROSPECTIVE", MODULE,
                    "Comparaison prospective entre le budget généré " + anneeBudget
                            + " et la prévision " + anneePrevision,
                    ENTITE, budget.getId() + "-" + prevision.getId(), JournalAuditService.SUCCES
            );

            return resultat;

        } catch (RuntimeException e) {
            journalAuditService.enregistrer(
                    "ECHEC_COMPARAISON_PROSPECTIVE", MODULE,
                    "Échec de comparaison entre " + anneeBudget + " et " + anneePrevision
                            + " : " + e.getMessage(),
                    ENTITE, null, JournalAuditService.ECHEC
            );
            throw e;
        }
    }

    private Budget budgetUnique(Integer annee, String type) {
        List<Budget> candidats = budgets.findAboutisParAnneeEtType(annee, type);

        if (candidats.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Aucun budget " + type + " pour " + annee);
        }

        if (candidats.size() > 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Plusieurs budgets " + type + " pour " + annee);
        }

        return candidats.get(0);
    }
}
