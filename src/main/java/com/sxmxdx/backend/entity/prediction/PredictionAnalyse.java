package com.sxmxdx.backend.entity.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Analyse textuelle produite par le pipeline pour un budget prévisionnel
 * (table {@code prediction_analyse}, une analyse par budget).
 */
@Entity
@Immutable
@Table(name = "prediction_analyse")
public class PredictionAnalyse {

    @Id
    @Column(name = "budget_id")
    private Long budgetId;

    private String resume;

    protected PredictionAnalyse() {
    }

    public Long getBudgetId() { return budgetId; }
    public String getResume() { return resume; }
}
