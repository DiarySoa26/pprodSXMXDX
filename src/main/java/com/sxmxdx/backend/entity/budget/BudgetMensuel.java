package com.sxmxdx.backend.entity.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/**
 * Ventilation mensuelle d'un {@link Budget} (table {@code budget_mensuel}).
 */
@Entity
@Immutable
@Table(name = "budget_mensuel")
public class BudgetMensuel {

    @Id
    private Long id;

    @Column(name = "budget_id")
    private Long budgetId;

    @Column(name = "mois_id")
    private Integer moisId;

    private BigDecimal ventes;

    private BigDecimal production;

    @Column(name = "ca_export")
    private BigDecimal caExport;

    @Column(name = "charges_exploitation")
    private BigDecimal chargesExploitation;

    @Column(name = "frais_export")
    private BigDecimal fraisExport;

    @Column(name = "masse_salariale")
    private BigDecimal masseSalariale;

    @Column(name = "resultat_operationnel")
    private BigDecimal resultatOperationnel;

    @Column(name = "marge_operationnelle")
    private BigDecimal margeOperationnelle;

    private BigDecimal amortissements;

    protected BudgetMensuel() {
    }

    public Long getId() { return id; }
    public Long getBudgetId() { return budgetId; }
    public Integer getMoisId() { return moisId; }
    public BigDecimal getVentes() { return ventes; }
    public BigDecimal getProduction() { return production; }
    public BigDecimal getCaExport() { return caExport; }
    public BigDecimal getChargesExploitation() { return chargesExploitation; }
    public BigDecimal getFraisExport() { return fraisExport; }
    public BigDecimal getMasseSalariale() { return masseSalariale; }
    public BigDecimal getResultatOperationnel() { return resultatOperationnel; }
    public BigDecimal getMargeOperationnelle() { return margeOperationnelle; }
    public BigDecimal getAmortissements() { return amortissements; }
}
