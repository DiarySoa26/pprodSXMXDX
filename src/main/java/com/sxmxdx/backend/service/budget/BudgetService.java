package com.sxmxdx.backend.service.budget;

import com.sxmxdx.backend.service.JournalAuditService;
import com.sxmxdx.backend.service.PipelineService;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class BudgetService {

    private static final String MODULE = "BUDGET";

    private final PipelineService pipelineService;
    private final JournalAuditService journalAuditService;

    public BudgetService(PipelineService pipelineService, JournalAuditService journalAuditService) {
        this.pipelineService = pipelineService;
        this.journalAuditService = journalAuditService;
    }

    public Map<?, ?> genererBudget() {
        try {
            Map<?, ?> resultat = pipelineService.genererBudget();
            boolean succes = PipelineService.estSucces(resultat);

            auditer(
                    succes ? "GENERATION_BUDGET" : "ECHEC_GENERATION_BUDGET",
                    succes ? "Génération du budget réussie." : "Échec de génération du budget.",
                    succes ? JournalAuditService.SUCCES : JournalAuditService.ECHEC
            );

            return resultat;

        } catch (RuntimeException e) {
            auditer("ECHEC_GENERATION_BUDGET", "Erreur : " + e.getMessage(), JournalAuditService.ECHEC);
            throw e;
        }
    }

    private void auditer(String action, String description, String statut) {
        journalAuditService.enregistrer(action, MODULE, description, MODULE, null, statut);
    }
}
