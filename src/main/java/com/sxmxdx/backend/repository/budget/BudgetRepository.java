package com.sxmxdx.backend.repository.budget;

import com.sxmxdx.backend.dto.prediction.PredictionResumeDTO;
import com.sxmxdx.backend.entity.budget.Budget;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByExerciceIdAndTypeBudgetOrderByIdDesc(Long exerciceId, String typeBudget);

    Optional<Budget> findFirstByExerciceIdAndTypeBudgetOrderByIdDesc(Long exerciceId, String typeBudget);

    Optional<Budget> findFirstByTypeBudgetOrderByIdDesc(String typeBudget);

    Optional<Budget> findByIdAndTypeBudget(Long id, String typeBudget);

    /** Budgets générés d'un exercice, du plus récemment généré au plus ancien. */
    @Query("""
            SELECT b FROM Budget b
            WHERE b.exerciceId = :exerciceId
              AND b.typeBudget = 'GENERE'
            ORDER BY b.dateGeneration DESC NULLS LAST, b.id DESC
            """)
    List<Budget> findGeneresParDateGeneration(@Param("exerciceId") Long exerciceId);

    /** Budgets d'un type donné, au statut GENERE, pour une année d'exercice. */
    @Query("""
            SELECT b FROM Budget b
            JOIN Exercice e ON e.id = b.exerciceId
            WHERE e.annee = :annee
              AND b.typeBudget = :type
              AND b.statut = 'GENERE'
            ORDER BY b.dateGeneration DESC, b.id DESC
            """)
    List<Budget> findAboutisParAnneeEtType(
            @Param("annee") Integer annee,
            @Param("type") String type
    );

    /** Années disposant d'un budget du type donné, au statut GENERE. */
    @Query("""
            SELECT DISTINCT e.annee FROM Budget b
            JOIN Exercice e ON e.id = b.exerciceId
            WHERE b.typeBudget = :type
              AND b.statut = 'GENERE'
            ORDER BY e.annee DESC
            """)
    List<Integer> findAnneesAbouties(@Param("type") String type);

    /** Années disposant d'au moins un budget généré. */
    @Query("""
            SELECT DISTINCT e.annee FROM Exercice e
            JOIN Budget b ON b.exerciceId = e.id
            WHERE b.typeBudget = 'GENERE'
            ORDER BY e.annee DESC
            """)
    List<Integer> findAnneesAvecBudgetGenere();

    /** Années disposant à la fois d'un budget généré et d'un budget prévisionnel. */
    @Query("""
            SELECT DISTINCT e.annee FROM Exercice e
            WHERE EXISTS (
                SELECT 1 FROM Budget b
                WHERE b.exerciceId = e.id AND b.typeBudget = 'GENERE'
            )
            AND EXISTS (
                SELECT 1 FROM Budget p
                WHERE p.exerciceId = e.id AND p.typeBudget = 'PREVISIONNEL'
            )
            ORDER BY e.annee
            """)
    List<Integer> findAnneesEvaluables();

    @Query("""
            SELECT new com.sxmxdx.backend.dto.prediction.PredictionResumeDTO(
                b.id, e.annee, b.anneeReference, b.statut, b.modele, b.dateGeneration
            )
            FROM Budget b
            JOIN Exercice e ON e.id = b.exerciceId
            WHERE b.typeBudget = 'PREVISIONNEL'
            ORDER BY e.annee DESC, b.id DESC
            """)
    List<PredictionResumeDTO> findResumesPredictions();
}
