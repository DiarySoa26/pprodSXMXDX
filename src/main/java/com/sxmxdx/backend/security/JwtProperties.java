package com.sxmxdx.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Paramètres de signature des jetons JWT ({@code security.jwt.*}).
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final int TAILLE_MINIMALE_CLE = 32;

    public JwtProperties {
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < TAILLE_MINIMALE_CLE) {
            throw new IllegalStateException(
                    "La variable JWT_SECRET doit contenir au moins "
                            + TAILLE_MINIMALE_CLE + " caractères."
            );
        }

        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            expiration = Duration.ofHours(8);
        }
    }
}
