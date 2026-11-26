package com.sxmxdx.backend.entity.budget;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Budget annuel (table {@code budget}), alimenté par le pipeline.
 * {@code typeBudget} vaut {@link #GENERE} (budget constaté) ou
 * {@link #PREVISIONNEL} (prédiction). Lecture seule côté backend.
 */
@Entity
@Immutable
@Table(name = "budget")
public class Budget {

    public static final String GENERE = "GENERE";
    public static final String PREVISIONNEL = "PREVISIONNEL";

    @Id
    private Long id;

    @Column(name = "exercice_id")
    private Long exerciceId;

    @Column(name = "import_id")
    private Long importId;

    @Column(name = "annee_reference")
    private Integer anneeReference;

    @Column(name = "type_budget")
    private String typeBudget;

    private String statut;

    private String modele;

    @Column(name = "version_modele")
    private String versionModele;

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

    @Column(name = "date_generation")
    private LocalDateTime dateGeneration;

    protected Budget() {
    }

    public Long getId() { return id; }
    public Long getExerciceId() { return exerciceId; }
    public Long getImportId() { return importId; }
    public Integer getAnneeReference() { return anneeReference; }
    public String getTypeBudget() { return typeBudget; }
    public String getStatut() { return statut; }
    public String getModele() { return modele; }
    public String getVersionModele() { return versionModele; }
    public BigDecimal getCaExport() { return caExport; }
    public BigDecimal getChargesExploitation() { return chargesExploitation; }
    public BigDecimal getFraisExport() { return fraisExport; }
    public BigDecimal getMasseSalariale() { return masseSalariale; }
    public BigDecimal getResultatOperationnel() { return resultatOperationnel; }
    public BigDecimal getMargeOperationnelle() { return margeOperationnelle; }
    public BigDecimal getAmortissements() { return amortissements; }
    public LocalDateTime getDateGeneration() { return dateGeneration; }
}
