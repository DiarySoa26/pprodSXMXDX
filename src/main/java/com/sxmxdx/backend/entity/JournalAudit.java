package com.sxmxdx.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "journal_audit")
public class JournalAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilisateur_id")
    private Integer utilisateurId;

    private String action;
    private String module;
    private String description;

    @Column(name = "entite_type")
    private String entiteType;

    @Column(name = "entite_id")
    private String entiteId;

    private String statut;

    @Column(name = "date_action")
    private LocalDateTime dateAction;

    @PrePersist
    void avantInsertion() {
        if (dateAction == null) {
            dateAction = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Integer getUtilisateurId() { return utilisateurId; }
    public void setUtilisateurId(Integer utilisateurId) { this.utilisateurId = utilisateurId; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getEntiteType() { return entiteType; }
    public void setEntiteType(String entiteType) { this.entiteType = entiteType; }

    public String getEntiteId() { return entiteId; }
    public void setEntiteId(String entiteId) { this.entiteId = entiteId; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public LocalDateTime getDateAction() { return dateAction; }
}
