package com.sxmxdx.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Sécurité des API : authentification sans état par jeton JWT (Bearer)
 * et contrôle d'accès par rôle.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String DAF = "DAF";
    private static final String COMPTABLE = "COMPTABLE";

    @Bean
    public SecurityFilterChain filtreSecurite(
            HttpSecurity http,
            UtilisateurActifJwtConverter convertisseur,
            ObjectMapper objectMapper
    ) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> { })
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(acces -> acces
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/test").permitAll()
                        .requestMatchers("/error").permitAll()

                        .requestMatchers("/api/utilisateurs/**").hasRole(ADMIN)
                        .requestMatchers("/api/tracabilite/**").hasAnyRole(ADMIN, DAF)

                        .requestMatchers(HttpMethod.POST, "/api/imports/**")
                            .hasAnyRole(ADMIN, COMPTABLE)
                        .requestMatchers(HttpMethod.POST, "/api/budgets/generer")
                            .hasAnyRole(ADMIN, COMPTABLE)
                        .requestMatchers(HttpMethod.POST, "/api/predictions/generer")
                            .hasAnyRole(ADMIN, DAF)
                        .requestMatchers(HttpMethod.POST, "/api/campagnes/**")
                            .hasAnyRole(ADMIN, COMPTABLE)
                        .requestMatchers(HttpMethod.PUT, "/api/campagnes/**")
                            .hasAnyRole(ADMIN, COMPTABLE)
                        .requestMatchers(HttpMethod.PATCH, "/api/campagnes/**")
                            .hasAnyRole(ADMIN, COMPTABLE)

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(serveur -> serveur
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(convertisseur))
                        .authenticationEntryPoint(nonAuthentifie(objectMapper))
                        .accessDeniedHandler(accesRefuse(objectMapper))
                )
                .exceptionHandling(erreurs -> erreurs
                        .authenticationEntryPoint(nonAuthentifie(objectMapper))
                        .accessDeniedHandler(accesRefuse(objectMapper))
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecretKey cleSignatureJwt(JwtProperties proprietes) {
        return new SecretKeySpec(
                proprietes.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey cle) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(cle));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey cle) {
        return NimbusJwtDecoder.withSecretKey(cle)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> originesAutorisees
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(originesAutorisees);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(
                List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        configuration.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private AuthenticationEntryPoint nonAuthentifie(ObjectMapper objectMapper) {
        return (requete, reponse, exception) -> {
            reponse.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            ecrireErreur(
                    reponse, objectMapper, HttpStatus.UNAUTHORIZED,
                    "Authentification requise ou session expirée."
            );
        };
    }

    private AccessDeniedHandler accesRefuse(ObjectMapper objectMapper) {
        return (requete, reponse, exception) -> ecrireErreur(
                reponse, objectMapper, HttpStatus.FORBIDDEN,
                "Vous n'avez pas les droits nécessaires pour cette action."
        );
    }

    private static void ecrireErreur(
            HttpServletResponse reponse,
            ObjectMapper objectMapper,
            HttpStatus statut,
            String message
    ) throws IOException {
        reponse.setStatus(statut.value());
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                reponse.getOutputStream(),
                Map.of("success", false, "message", message)
        );
    }
}
