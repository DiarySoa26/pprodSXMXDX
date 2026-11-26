package com.sxmxdx.backend.service.budgetdetails;

import com.sxmxdx.backend.repository.budgetdetails.BudgetDetailsRepository;
import com.sxmxdx.backend.service.JournalAuditService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Consultation des détails budgétaires d'un exercice. Chaque consultation
 * est tracée dans la piste d'audit.
 */
@Service
@Transactional(readOnly = true)
public class BudgetDetailsService {

    private static final int NB_MOIS = 12;

    private final BudgetDetailsRepository repository;
    private final JournalAuditService journalAuditService;
    private final CompteResultatCalculService compteResultatCalculService;

    public BudgetDetailsService(
            BudgetDetailsRepository repository,
            JournalAuditService journalAuditService,
            CompteResultatCalculService compteResultatCalculService
    ) {
        this.repository = repository;
        this.journalAuditService = journalAuditService;
        this.compteResultatCalculService = compteResultatCalculService;
    }

    public List<Map<String, Object>> getCharges(Integer exerciceId) {
        return consulter("CHARGES", exerciceId, repository::getCharges);
    }

    public List<Map<String, Object>> getExportations(Integer exerciceId) {
        return consulter("EXPORTATIONS", exerciceId, repository::getExportations);
    }

    public List<Map<String, Object>> getFraisExport(Integer exerciceId) {
        return consulter("FRAIS_EXPORT", exerciceId, repository::getFraisExport);
    }

    public List<Map<String, Object>> getProduction(Integer exerciceId) {
        return consulter("PRODUCTION", exerciceId, repository::getProduction);
    }

    public List<Map<String, Object>> getEmployes(Integer exerciceId) {
        return consulter("EMPLOYES", exerciceId, repository::getEmployes);
    }

    public List<Map<String, Object>> getBudget(Integer exerciceId) {
        return consulter("BUDGET", exerciceId, repository::getBudget);
    }

    public List<Map<String, Object>> getBudgetMensuel(Integer budgetId) {
        return consulter("BUDGET_MENSUEL", budgetId, repository::getBudgetMensuel);
    }

    public List<Map<String, Object>> getAmortissements(Integer exerciceId) {
        return consulter("AMORTISSEMENTS", exerciceId, repository::getAmortissements);
    }

    public List<Map<String, Object>> getAmortissementsTotaux(Integer exerciceId) {
        return consulter("AMORTISSEMENTS_TOTAUX", exerciceId, repository::getAmortissementsTotaux);
    }

    public List<Map<String, Object>> getAmortissementsAnnuel(Integer exerciceId) {
        return consulter("AMORTISSEMENTS_ANNUEL", exerciceId, repository::getAmortissementsAnnuel);
    }

    public List<Map<String, Object>> getCompteResultat(Integer exerciceId) {
        return consulter("COMPTE_RESULTAT", exerciceId, id ->
                compteResultatCalculService.calculerCompteResultat(donneesCompteResultat(id)));
    }

    private <T> T consulter(String type, Integer entiteId, Function<Integer, T> lecture) {
        T resultat = lecture.apply(entiteId);

        journalAuditService.enregistrer(
                "CONSULTATION_" + type, "CONSULTATION", "Consultation " + type,
                type, String.valueOf(entiteId), JournalAuditService.SUCCES
        );

        return resultat;
    }

    /** Rubriques du compte de résultat alimentées par les données de l'exercice. */
    private Map<String, BigDecimal[]> donneesCompteResultat(Integer exerciceId) {
        List<Map<String, Object>> budgetMensuel = repository.getBudgetMensuelPourCR(exerciceId);

        Map<String, BigDecimal[]> donnees = new LinkedHashMap<>();
        donnees.put("CA", montantsMensuels(budgetMensuel, "mois_id", "ca_export"));
        donnees.put("PERSONNEL", montantsMensuels(budgetMensuel, "mois_id", "masse_salariale"));
        donnees.put("AMORTISSEMENTS", montantsMensuels(
                repository.getAmortissementsPourCR(exerciceId), "mois", "montant"));
        return donnees;
    }

    private static BigDecimal[] montantsMensuels(
            List<Map<String, Object>> lignes,
            String colonneMois,
            String colonneMontant
    ) {
        BigDecimal[] resultat = new BigDecimal[NB_MOIS];
        Arrays.fill(resultat, BigDecimal.ZERO);

        for (Map<String, Object> ligne : lignes) {
            if (!(ligne.get(colonneMois) instanceof Number numeroMois)) {
                throw new IllegalStateException("Numéro de mois invalide : " + ligne.get(colonneMois));
            }

            int mois = numeroMois.intValue();

            if (mois < 1 || mois > NB_MOIS) {
                throw new IllegalStateException("Mois hors limites : " + mois);
            }

            Object montant = ligne.get(colonneMontant);

            if (montant != null) {
                resultat[mois - 1] = resultat[mois - 1].add(new BigDecimal(montant.toString()));
            }
        }

        return resultat;
    }
}
