package com.sxmxdx.backend.repository.exercice;

import com.sxmxdx.backend.dto.exercice.ExerciceDTO;
import com.sxmxdx.backend.entity.exercice.ExerciceView;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lecture de la vue {@code view_exercice} (un exercice × ses imports).
 * <p>
 * Un exercice peut apparaître sur plusieurs lignes (une par import) : les
 * requêtes passent donc par des projections, jamais par le chargement
 * d'entités, pour que Hibernate ne fusionne pas les lignes de même exercice.
 */
public interface ExerciceViewRepository extends JpaRepository<ExerciceView, Integer> {

    String PROJECTION_DTO = """
            SELECT new com.sxmxdx.backend.dto.exercice.ExerciceDTO(
                v.idExercice, v.anneeExercice, v.idImportFichier, v.nomFichier,
                v.exerciceId, v.statut, v.dateImport, v.messageErreur
            )
            FROM ExerciceView v
            """;

    @Query(PROJECTION_DTO + " ORDER BY v.anneeExercice DESC")
    List<ExerciceDTO> findTous();

    @Query(PROJECTION_DTO + " WHERE v.anneeExercice = :annee")
    List<ExerciceDTO> findParAnnee(@Param("annee") Integer annee);

    @Query("""
            SELECT DISTINCT v.anneeExercice FROM ExerciceView v
            WHERE v.anneeExercice IS NOT NULL
            ORDER BY v.anneeExercice DESC
            """)
    List<Integer> findAnnees();

    @Query("""
            SELECT v.dateImport FROM ExerciceView v
            WHERE v.idImportFichier = :importId
              AND v.idExercice = :exerciceId
            """)
    List<LocalDateTime> findDateImport(
            @Param("importId") Integer importId,
            @Param("exerciceId") Integer exerciceId
    );
}
