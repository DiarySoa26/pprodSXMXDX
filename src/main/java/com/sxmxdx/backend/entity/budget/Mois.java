package com.sxmxdx.backend.entity.budget;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.Immutable;

/**
 * Référentiel des mois (table {@code mois}).
 */
@Entity
@Immutable
@Table(name = "mois")
public class Mois {

    @Id
    private Integer id;

    private String nom;

    protected Mois() {
    }

    public Integer getId() { return id; }
    public String getNom() { return nom; }
}
