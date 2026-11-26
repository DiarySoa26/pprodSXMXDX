package com.sxmxdx.backend.service.comparaison;

import com.sxmxdx.backend.entity.budget.Budget;

import java.math.BigDecimal;
import java.util.function.Function;

/**
 * Indicateurs financiers comparés entre deux budgets.
 * Les montants sont en MGA ; la marge est un ratio exprimé en points.
 */
enum IndicateurFinancier {

    CA_EXPORT("Chiffre d'affaires export", Budget::getCaExport),
    CHARGES("Charges d'exploitation", Budget::getChargesExploitation),
    FRAIS_EXPORT("Frais d'exportation", Budget::getFraisExport),
    MASSE_SALARIALE("Masse salariale", Budget::getMasseSalariale),
    RESULTAT("Résultat opérationnel", Budget::getResultatOperationnel),
    MARGE("Marge opérationnelle", Budget::getMargeOperationnelle);

    static final String UNITE_MONTANT = "MGA";
    static final String UNITE_POINT = "POINT";

    private final String libelle;
    private final Function<Budget, BigDecimal> extracteur;

    IndicateurFinancier(String libelle, Function<Budget, BigDecimal> extracteur) {
        this.libelle = libelle;
        this.extracteur = extracteur;
    }

    String libelle() {
        return libelle;
    }

    String unite() {
        return this == MARGE ? UNITE_POINT : UNITE_MONTANT;
    }

    boolean enPoints() {
        return this == MARGE;
    }

    BigDecimal valeur(Budget budget) {
        return extracteur.apply(budget);
    }
}
