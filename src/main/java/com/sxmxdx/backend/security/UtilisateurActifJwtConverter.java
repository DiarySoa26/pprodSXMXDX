package com.sxmxdx.backend.security;

import com.sxmxdx.backend.entity.Utilisateur;
import com.sxmxdx.backend.repository.UtilisateurRepository;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Transforme un jeton JWT valide en authentification Spring Security.
 * <p>
 * L'utilisateur est relu en base à chaque requête : un compte désactivé
 * ou un changement de rôle prend effet immédiatement, sans attendre
 * l'expiration du jeton.
 */
@Component
public class UtilisateurActifJwtConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UtilisateurRepository utilisateurs;

    public UtilisateurActifJwtConverter(UtilisateurRepository utilisateurs) {
        this.utilisateurs = utilisateurs;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Utilisateur utilisateur = utilisateurs.findById(identifiant(jwt))
                .orElseThrow(() -> new BadCredentialsException("Utilisateur inconnu."));

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            throw new DisabledException("Ce compte est désactivé.");
        }

        String role = "ROLE_" + utilisateur.getRole().getNom().toUpperCase(Locale.ROOT);

        return new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority(role)),
                String.valueOf(utilisateur.getId())
        );
    }

    private Integer identifiant(Jwt jwt) {
        try {
            return Integer.valueOf(jwt.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new BadCredentialsException("Jeton invalide.");
        }
    }
}
