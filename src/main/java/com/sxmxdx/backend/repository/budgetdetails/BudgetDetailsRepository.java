package com.sxmxdx.backend.repository.budgetdetails;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lecture des vues SQL de détail budgétaire ({@code view_*_mois}...).
 * <p>
 * Ces vues sont produites par le pipeline et leurs colonnes sont renvoyées
 * telles quelles au frontend : elles sont donc lues par requêtes natives JPA
 * en lignes génériques plutôt que mappées sur des entités.
 */
@Repository
public class BudgetDetailsRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Map<String, Object>> getCharges(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM view_charges_mois
                WHERE exercice_id = :exerciceId
                ORDER BY mois_id, id
                """, exerciceId);
    }

    public List<Map<String, Object>> getExportations(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM view_exportations_mois
                WHERE exercice_id = :exerciceId
                ORDER BY mois_id, id
                """, exerciceId);
    }

    public List<Map<String, Object>> getFraisExport(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM view_frais_export_mois
                WHERE exercice_id = :exerciceId
                ORDER BY mois_id, id
                """, exerciceId);
    }

    public List<Map<String, Object>> getProduction(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM view_production_mois
                WHERE exercice_id = :exerciceId
                ORDER BY mois_id, id
                """, exerciceId);
    }

    public List<Map<String, Object>> getEmployes(Integer exerciceId) {
        return parExercice("""
                SELECT employe.* FROM employe
                JOIN view_exercice ve ON ve.id = employe.import_id
                WHERE ve.exercice_id = :exerciceId
                ORDER BY employe.id
                """, exerciceId);
    }

    public List<Map<String, Object>> getBudget(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM budget
                WHERE exercice_id = :exerciceId
                  AND type_budget = 'GENERE'
                ORDER BY id DESC
                """, exerciceId);
    }

    public List<Map<String, Object>> getBudgetMensuel(Integer budgetId) {
        return executer("""
                SELECT * FROM view_budget_mensuel_mois
                WHERE budget_id = :budgetId
                ORDER BY id
                """, Map.of("budgetId", budgetId));
    }

    public List<Map<String, Object>> getAmortissements(Integer exerciceId) {
        return parExercice("""
                SELECT * FROM view_amortissement_mois
                WHERE exercice_id = :exerciceId
                ORDER BY mois, id
                """, exerciceId);
    }

    public List<Map<String, Object>> getAmortissementsTotaux(Integer exerciceId) {
        return parExercice("""
                SELECT mois, nom_mois, SUM(montant) AS total_amortissement
                FROM view_amortissement_mois
                WHERE exercice_id = :exerciceId
                GROUP BY mois, nom_mois
                ORDER BY mois
                """, exerciceId);
    }

    public List<Map<String, Object>> getAmortissementsAnnuel(Integer exerciceId) {
        return parExercice("""
                SELECT COALESCE(SUM(montant), 0) AS total_annuel
                FROM view_amortissement_mois
                WHERE exercice_id = :exerciceId
                """, exerciceId);
    }

    /** Amortissements mensuels d'un exercice, pour le compte de résultat. */
    public List<Map<String, Object>> getAmortissementsPourCR(Integer exerciceId) {
        return parExercice("""
                SELECT mois, COALESCE(SUM(montant), 0) AS montant
                FROM amortissement_mensuel
                WHERE exercice_id = :exerciceId
                GROUP BY mois
                ORDER BY mois
                """, exerciceId);
    }

    /** Ventilation mensuelle du dernier budget généré, pour le compte de résultat. */
    public List<Map<String, Object>> getBudgetMensuelPourCR(Integer exerciceId) {
        return parExercice("""
                SELECT bm.* FROM view_budget_mensuel_mois bm
                WHERE bm.budget_id = (
                    SELECT MAX(b.id) FROM budget b
                    WHERE b.exercice_id = :exerciceId
                      AND b.type_budget = 'GENERE'
                )
                ORDER BY bm.mois_id
                """, exerciceId);
    }

    private List<Map<String, Object>> parExercice(String sql, Integer exerciceId) {
        return executer(sql, Map.of("exerciceId", exerciceId));
    }

    private List<Map<String, Object>> executer(String sql, Map<String, Object> parametres) {
        var requete = entityManager.createNativeQuery(sql, Tuple.class);
        parametres.forEach(requete::setParameter);

        @SuppressWarnings("unchecked")
        List<Tuple> lignes = requete.getResultList();

        return lignes.stream().map(BudgetDetailsRepository::versMap).toList();
    }

    private static Map<String, Object> versMap(Tuple tuple) {
        Map<String, Object> ligne = new LinkedHashMap<>();
        for (TupleElement<?> colonne : tuple.getElements()) {
            ligne.put(colonne.getAlias(), tuple.get(colonne.getAlias()));
        }
        return ligne;
    }
}
