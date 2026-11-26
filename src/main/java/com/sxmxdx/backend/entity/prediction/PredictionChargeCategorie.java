package com.sxmxdx.backend.entity.prediction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Charge prévue par catégorie et par mois pour un budget prévisionnel.
 */
@Entity
@Immutable
@Table(name = "prediction_charge_categorie")
public class PredictionChargeCategorie {

    @Id
    private Long id;

    @Column(name = "budget_id", nullable = false)
    private Long budgetId;

    @Column(name = "mois_id", nullable = false)
    private Integer moisId;

    @Column(nullable = false)
    private String categorie;

    @Column(name = "montant_prevu", nullable = false)
    private BigDecimal montantPrevu;

    protected PredictionChargeCategorie() {
    }

    public Long getId() { return id; }
    public Long getBudgetId() { return budgetId; }
    public Integer getMoisId() { return moisId; }
    public String getCategorie() { return categorie; }
    public BigDecimal getMontantPrevu() { return montantPrevu; }
}
