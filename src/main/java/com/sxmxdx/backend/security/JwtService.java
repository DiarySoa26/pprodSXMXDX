package com.sxmxdx.backend.security;

import com.sxmxdx.backend.entity.Utilisateur;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Émet les jetons d'accès remis à l'utilisateur après connexion.
 * Le sujet ({@code sub}) du jeton est l'identifiant de l'utilisateur.
 */
@Service
public class JwtService {

    static final String CLAIM_ROLE = "role";
    static final String CLAIM_EMAIL = "email";

    private static final String EMETTEUR = "sxmxdx-backend";

    private final JwtEncoder encoder;
    private final JwtProperties proprietes;

    public JwtService(JwtEncoder encoder, JwtProperties proprietes) {
        this.encoder = encoder;
        this.proprietes = proprietes;
    }

    public String genererJeton(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(EMETTEUR)
                .issuedAt(maintenant)
                .expiresAt(maintenant.plus(proprietes.expiration()))
                .subject(String.valueOf(utilisateur.getId()))
                .claim(CLAIM_EMAIL, utilisateur.getEmail())
                .claim(CLAIM_ROLE, utilisateur.getRole().getNom())
                .build();

        JwsHeader entete = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder
                .encode(JwtEncoderParameters.from(entete, claims))
                .getTokenValue();
    }

    public long dureeValiditeSecondes() {
        return proprietes.expiration().toSeconds();
    }
}
