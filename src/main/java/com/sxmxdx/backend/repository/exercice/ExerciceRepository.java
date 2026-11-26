package com.sxmxdx.backend.repository.exercice;

import com.sxmxdx.backend.entity.exercice.Exercice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    Optional<Exercice> findByAnnee(Integer annee);

    boolean existsByIdAndAnnee(Long id, Integer annee);
}
