package com.sxmxdx.backend.service.comparaison;

import com.sxmxdx.backend.dto.comparaison.IndicateurComparaisonDTO;
import com.sxmxdx.backend.entity.budget.Budget;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;

/**
 * Calcule l'évolution des indicateurs financiers d'un budget de référence
 * vers un budget cible, et les commente.
 */
@Component
class ComparateurBudgets {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    static final String AUGMENTATION = "AUGMENTATION";
    static final String DIMINUTION = "DIMINUTION";
    static final String STABLE = "STABLE";

    List<IndicateurComparaisonDTO> comparer(Budget reference, Budget cible) {
        return Arrays.stream(IndicateurFinancier.values())
                .map(indicateur -> comparer(
                        indicateur,
                        indicateur.valeur(reference),
                        indicateur.valeur(cible)
                ))
                .toList();
    }

    List<String> interpreter(
            List<IndicateurComparaisonDTO> indicateurs,
            Integer anneeReference,
            Integer anneeCible
    ) {
        return indicateurs.stream()
                .map(indicateur -> interpreter(indicateur, anneeReference, anneeCible))
                .toList();
    }

    private IndicateurComparaisonDTO comparer(
            IndicateurFinancier indicateur,
            BigDecimal valeurReference,
            BigDecimal valeurCible
    ) {
        BigDecimal reference = sansNull(valeurReference);
        BigDecimal cible = sansNull(valeurCible);
        BigDecimal ecart = cible.subtract(reference);

        return new IndicateurComparaisonDTO(
                indicateur.name(),
                indicateur.libelle(),
                indicateur.unite(),
                reference,
                cible,
                ecart,
                evolution(indicateur, reference, ecart),
                tendance(ecart)
        );
    }

    /**
     * Évolution en % pour un montant, en points pour un ratio ;
     * {@code null} si la référence est nulle (variation non calculable).
     */
    private static BigDecimal evolution(
            IndicateurFinancier indicateur,
            BigDecimal reference,
            BigDecimal ecart
    ) {
        if (indicateur.enPoints()) {
            return ecart.multiply(CENT).setScale(2, RoundingMode.HALF_UP);
        }

        if (reference.signum() == 0) {
            return null;
        }

        return ecart.multiply(CENT).divide(reference.abs(), 2, RoundingMode.HALF_UP);
    }

    private static String tendance(BigDecimal ecart) {
        return switch (ecart.signum()) {
            case 1 -> AUGMENTATION;
            case -1 -> DIMINUTION;
            default -> STABLE;
        };
    }

    private static String interpreter(
            IndicateurComparaisonDTO indicateur,
            Integer anneeReference,
            Integer anneeCible
    ) {
        if (indicateur.evolutionPourcentage() == null) {
            return indicateur.libelle()
                    + " : variation relative non calculable (référence nulle).";
        }

        String mouvement = switch (indicateur.tendance()) {
            case AUGMENTATION -> "augmente";
            case DIMINUTION -> "diminue";
            default -> "reste stable";
        };

        String unite = IndicateurFinancier.UNITE_POINT.equals(indicateur.unite())
                ? " point(s)"
                : " %";

        return indicateur.libelle() + " " + mouvement + " de "
                + indicateur.evolutionPourcentage().abs().toPlainString() + unite
                + " entre " + anneeReference + " et " + anneeCible + ".";
    }

    private static BigDecimal sansNull(BigDecimal valeur) {
        return valeur == null ? BigDecimal.ZERO : valeur;
    }
}
