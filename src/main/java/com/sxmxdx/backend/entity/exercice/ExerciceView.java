package com.sxmxdx.backend.entity.exercice;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

/**
 * Vue {@code view_exercice} : un exercice et ses fichiers importés.
 * <p>
 * Attention : {@code id_exercice} n'est pas unique dans la vue (une ligne par
 * import). Ne lire cette vue que par projection — voir
 * {@link com.sxmxdx.backend.repository.exercice.ExerciceViewRepository}.
 */
@Entity
@Immutable
@Table(name = "view_exercice")
public class ExerciceView {

    @Id
    @Column(name = "id_exercice")
    private Integer idExercice;

    @Column(name = "annee_exercice")
    private Integer anneeExercice;

    @Column(name = "id")
    private Integer idImportFichier;

    @Column(name = "nom_fichier")
    private String nomFichier;

    @Column(name = "exercice_id")
    private Integer exerciceId;

    private String statut;

    @Column(name = "date_import")
    private LocalDateTime dateImport;

    @Column(name = "message_erreur")
    private String messageErreur;

    protected ExerciceView() {
    }

    public Integer getIdExercice() { return idExercice; }
    public Integer getAnneeExercice() { return anneeExercice; }
    public Integer getIdImportFichier() { return idImportFichier; }
    public String getNomFichier() { return nomFichier; }
    public Integer getExerciceId() { return exerciceId; }
    public String getStatut() { return statut; }
    public LocalDateTime getDateImport() { return dateImport; }
    public String getMessageErreur() { return messageErreur; }
}
