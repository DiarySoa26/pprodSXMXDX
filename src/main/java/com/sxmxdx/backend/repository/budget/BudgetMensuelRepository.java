package com.sxmxdx.backend.repository.budget;

import com.sxmxdx.backend.dto.prediction.PredictionMensuelleDTO;
import com.sxmxdx.backend.entity.budget.BudgetMensuel;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BudgetMensuelRepository extends JpaRepository<BudgetMensuel, Long> {

    List<BudgetMensuel> findByBudgetIdOrderByMoisIdAsc(Long budgetId);

    @Query("""
            SELECT new com.sxmxdx.backend.dto.prediction.PredictionMensuelleDTO(
                bm.moisId, m.nom, bm.ventes, bm.production, bm.caExport,
                bm.chargesExploitation, bm.fraisExport, bm.masseSalariale,
                bm.resultatOperationnel, bm.margeOperationnelle
            )
            FROM BudgetMensuel bm
            JOIN Mois m ON m.id = bm.moisId
            WHERE bm.budgetId = :budgetId
            ORDER BY bm.moisId
            """)
    List<PredictionMensuelleDTO> findDetailMensuel(@Param("budgetId") Long budgetId);
}
