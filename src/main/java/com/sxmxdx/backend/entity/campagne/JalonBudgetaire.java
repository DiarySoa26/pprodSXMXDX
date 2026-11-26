package com.sxmxdx.backend.entity.campagne;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "jalon_budgetaire")
public class JalonBudgetaire {

    public static final String EN_ATTENTE = "EN_ATTENTE";
    public static final String TERMINE = "TERMINE";

    public static final String IMPORTATION = "IMPORTATION";
    public static final String GENERATION = "GENERATION";
    public static final String VERIFICATION = "VERIFICATION";
    public static final String VALIDATION = "VALIDATION";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campagne_id", nullable = false)
    private Long campagneId;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false)
    private Integer ordre;

    @Column(name = "date_limite", nullable = false)
    private LocalDate dateLimite;

    @Column(name = "date_realisation")
    private LocalDateTime dateRealisation;

    @Column(nullable = false, length = 20)
    private String statut = EN_ATTENTE;

    public JalonBudgetaire() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCampagneId() { return campagneId; }
    public void setCampagneId(Long campagneId) { this.campagneId = campagneId; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getOrdre() { return ordre; }
    public void setOrdre(Integer ordre) { this.ordre = ordre; }

    public LocalDate getDateLimite() { return dateLimite; }
    public void setDateLimite(LocalDate dateLimite) { this.dateLimite = dateLimite; }

    public LocalDateTime getDateRealisation() { return dateRealisation; }
    public void setDateRealisation(LocalDateTime dateRealisation) {
        this.dateRealisation = dateRealisation;
    }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public boolean estTermine() {
        return TERMINE.equals(statut);
    }

    /** Marque le jalon comme terminé maintenant (sans effet s'il l'est déjà). */
    public void terminer() {
        if (!estTermine()) {
            statut = TERMINE;
            dateRealisation = LocalDateTime.now();
        }
    }
}
