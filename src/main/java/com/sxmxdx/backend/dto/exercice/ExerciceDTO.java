package com.sxmxdx.backend.dto.exercice;

import java.time.LocalDateTime;

public record ExerciceDTO(
        Integer idExercice,
        Integer anneeExercice,
        Integer idImportFichier,
        String nomFichier,
        Integer exerciceId,
        String statut,
        LocalDateTime dateImport,
        String messageErreur
) {
}
