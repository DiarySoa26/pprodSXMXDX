package com.sxmxdx.backend.security;

import com.sxmxdx.backend.entity.Role;
import com.sxmxdx.backend.entity.Utilisateur;
import com.sxmxdx.backend.repository.JournalConnexionRepository;
import com.sxmxdx.backend.repository.UtilisateurRepository;
import com.sxmxdx.backend.service.JournalAuditService;
import com.sxmxdx.backend.service.UtilisateurService;
import com.sxmxdx.backend.service.budget.BudgetService;
import com.sxmxdx.backend.service.exercice.ExerciceService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie l'authentification JWT et le contrôle d'accès par rôle,
 * sans base de données (les accès aux données sont simulés).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/inexistante",
        "spring.datasource.password=test",
        "spring.datasource.hikari.initialization-fail-timeout=-1",
        "spring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false",
        "security.jwt.secret=" + SecuriteApiTest.SECRET
})
@AutoConfigureMockMvc
class SecuriteApiTest {

    static final String SECRET = "cle-de-test-suffisamment-longue-pour-hs256";

    private static final int ID_ADMIN = 1;
    private static final int ID_LECTEUR = 2;
    private static final int ID_DESACTIVE = 3;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private UtilisateurRepository utilisateurs;

    @MockitoBean
    private JournalConnexionRepository journalConnexions;

    @MockitoBean
    private JournalAuditService journalAudit;

    @MockitoBean
    private ExerciceService exerciceService;

    @MockitoBean
    private BudgetService budgetService;

    @MockitoBean
    private UtilisateurService utilisateurService;

    private Utilisateur admin;
    private Utilisateur lecteur;
    private Utilisateur desactive;

    @BeforeEach
    void preparer() {
        admin = utilisateur(ID_ADMIN, "admin@test.mg", "ADMIN", true);
        lecteur = utilisateur(ID_LECTEUR, "lecteur@test.mg", "LECTEUR", true);
        desactive = utilisateur(ID_DESACTIVE, "ancien@test.mg", "ADMIN", false);

        for (Utilisateur u : List.of(admin, lecteur, desactive)) {
            when(utilisateurs.findById(u.getId())).thenReturn(Optional.of(u));
            when(utilisateurs.findByEmailIgnoreCase(u.getEmail())).thenReturn(Optional.of(u));
        }
        when(utilisateurs.findByEmailIgnoreCase(anyString()))
                .thenAnswer(appel -> List.of(admin, lecteur, desactive).stream()
                        .filter(u -> u.getEmail().equalsIgnoreCase(appel.getArgument(0)))
                        .findFirst());

        when(exerciceService.rechercher(null)).thenReturn(List.of());
        when(utilisateurService.lister()).thenReturn(List.of());
    }

    // --- Accès public ------------------------------------------------------

    @Test
    void lePointDeControleEstPublic() throws Exception {
        mvc.perform(get("/api/test")).andExpect(status().isOk());
    }

    @Test
    void uneApiSansJetonRenvoie401() throws Exception {
        mvc.perform(get("/api/exercices"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.success").value(false));
    }

    // --- Connexion ---------------------------------------------------------

    @Test
    void uneConnexionReussieRenvoieUnJetonUtilisable() throws Exception {
        String reponse = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.mg\",\"password\":\"motdepasse\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.id").value(ID_ADMIN))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        String jeton = reponse.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mvc.perform(get("/api/exercices").header(HttpHeaders.AUTHORIZATION, "Bearer " + jeton))
                .andExpect(status().isOk());
    }

    @Test
    void unMauvaisMotDePasseRenvoie401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.mg\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void unEmailInconnuRenvoieLeMemeMessageQuUnMauvaisMotDePasse() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inconnu@test.mg\",\"password\":\"motdepasse\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void unCompteDesactiveNEstSignaleQuAvecLeBonMotDePasse() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ancien@test.mg\",\"password\":\"faux\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ancien@test.mg\",\"password\":\"motdepasse\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Ce compte est désactivé."));
    }

    // --- Jetons ------------------------------------------------------------

    @Test
    void unJetonSigneAvecUneAutreCleEstRefuse() throws Exception {
        mvc.perform(get("/api/exercices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jetonForge(ID_ADMIN)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void leJetonDUnCompteDesactiveDepuisSaConnexionEstRefuse() throws Exception {
        mvc.perform(get("/api/exercices").header(HttpHeaders.AUTHORIZATION, bearer(desactive)))
                .andExpect(status().isUnauthorized());
    }

    // --- Rôles -------------------------------------------------------------

    @Test
    void unLecteurPeutConsulter() throws Exception {
        mvc.perform(get("/api/exercices").header(HttpHeaders.AUTHORIZATION, bearer(lecteur)))
                .andExpect(status().isOk());
    }

    @Test
    void unLecteurNePeutPasGenererDeBudget() throws Exception {
        mvc.perform(post("/api/budgets/generer").header(HttpHeaders.AUTHORIZATION, bearer(lecteur)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));

        verify(budgetService, never()).genererBudget();
    }

    @Test
    void seulUnAdministrateurGereLesUtilisateurs() throws Exception {
        mvc.perform(get("/api/utilisateurs").header(HttpHeaders.AUTHORIZATION, bearer(lecteur)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/utilisateurs").header(HttpHeaders.AUTHORIZATION, bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void lAuteurEnvoyeParLeClientEstIgnore() throws Exception {
        when(utilisateurService.changerStatut(eq(5), eq(false))).thenReturn(null);

        // L'ancien paramètre auteurId est ignoré : l'auteur vient du jeton
        mvc.perform(patch("/api/utilisateurs/5/statut?auteurId=999")
                        .header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actif\":false}"))
                .andExpect(status().isOk());

        verify(utilisateurService).changerStatut(5, false);
    }

    // --- Outils ------------------------------------------------------------

    private String bearer(Utilisateur utilisateur) {
        return "Bearer " + jwtService.genererJeton(utilisateur);
    }

    private static String jetonForge(int idUtilisateur) {
        var cle = new SecretKeySpec(
                "une-toute-autre-cle-secrete-de-32-octets!".getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
        var claims = JwtClaimsSet.builder()
                .subject(String.valueOf(idUtilisateur))
                .claims(c -> c.putAll(Map.of("role", "ADMIN")))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        return new NimbusJwtEncoder(new ImmutableSecret<>(cle))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private Utilisateur utilisateur(int id, String email, String nomRole, boolean actif) {
        Role role = new Role();
        role.setNom(nomRole);

        Utilisateur u = new Utilisateur();
        u.setId(id);
        u.setNom("Test");
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode("motdepasse"));
        u.setRole(role);
        u.setActif(actif);
        return u;
    }
}
