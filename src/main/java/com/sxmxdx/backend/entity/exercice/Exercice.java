package com.sxmxdx.backend.entity.exercice;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Exercice comptable (table {@code exercice}).
 */
@Entity
@Immutable
@Table(name = "exercice")
public class Exercice {

    @Id
    private Long id;

    private Integer annee;

    protected Exercice() {
    }

    public Long getId() { return id; }
    public Integer getAnnee() { return annee; }
}
