package com.sxmxdx.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

/**
 * Paramètres de signature des jetons JWT ({@code security.jwt.*}).
 *
 * <p>Si {@code JWT_SECRET} est absent ou trop court, une clé aléatoire forte est
 * générée au démarrage : l'application démarre, mais les jetons émis deviennent
 * invalides à chaque redémarrage. Définir {@code JWT_SECRET} (≥ 32 caractères)
 * en production.</p>
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration) {

    private static final Logger LOG = LoggerFactory.getLogger(JwtProperties.class);

    private static final int TAILLE_MINIMALE_CLE = 32;

    public JwtProperties {
        if (secret == null
                || secret.getBytes(StandardCharsets.UTF_8).length < TAILLE_MINIMALE_CLE) {
            LOG.warn("JWT_SECRET absent ou inférieur à {} caractères : génération d'une clé "
                    + "aléatoire temporaire. Les jetons seront invalidés à chaque redémarrage. "
                    + "Définissez JWT_SECRET dans les variables d'environnement.",
                    TAILLE_MINIMALE_CLE);
            secret = genererCleAleatoire();
        }

        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            expiration = Duration.ofHours(8);
        }
    }

    private static String genererCleAleatoire() {
        byte[] octets = new byte[48];
        new SecureRandom().nextBytes(octets);
        return Base64.getEncoder().encodeToString(octets);
    }
}
