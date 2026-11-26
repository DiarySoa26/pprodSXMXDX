package com.sxmxdx.backend.service;

import com.sxmxdx.backend.dto.UtilisateurDTO;
import com.sxmxdx.backend.dto.UtilisateurRequest;
import com.sxmxdx.backend.entity.Role;
import com.sxmxdx.backend.entity.Utilisateur;
import com.sxmxdx.backend.repository.RoleRepository;
import com.sxmxdx.backend.repository.UtilisateurRepository;
import com.sxmxdx.backend.security.UtilisateurCourant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class UtilisateurService {

    private static final String MODULE = "UTILISATEURS";
    private static final String ENTITE = "UTILISATEUR";
    private static final String ROLE_ADMIN = "ADMIN";

    private static final Set<String> ROLES = Set.of(ROLE_ADMIN, "DAF", "COMPTABLE", "LECTEUR");
    private static final Pattern FORMAT_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int LONGUEUR_MIN_MOT_DE_PASSE = 8;

    private final UtilisateurRepository utilisateurs;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JournalAuditService journalAuditService;

    public UtilisateurService(
            UtilisateurRepository utilisateurs,
            RoleRepository roles,
            PasswordEncoder encoder,
            JournalAuditService journalAuditService
    ) {
        this.utilisateurs = utilisateurs;
        this.roles = roles;
        this.encoder = encoder;
        this.journalAuditService = journalAuditService;
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDTO> lister() {
        return utilisateurs.findAll().stream()
                .map(UtilisateurDTO::depuis)
                .toList();
    }

    @Transactional
    public UtilisateurDTO creer(UtilisateurRequest requete) {
        exigerRequete(requete);

        String email = verifierEmail(requete.email());
        verifierEmailDisponible(email, null);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(verifierNom(requete.nom()));
        utilisateur.setPrenom(nettoyer(requete.prenom()));
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(encoder.encode(verifierMotDePasse(requete.motDePasse())));
        utilisateur.setRole(trouverRole(requete.role()));
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());

        Utilisateur enregistre = utilisateurs.save(utilisateur);

        auditer(
                "CREATION_UTILISATEUR",
                "Création de l'utilisateur : " + enregistre.getEmail(),
                enregistre
        );

        return UtilisateurDTO.depuis(enregistre);
    }

    @Transactional
    public UtilisateurDTO modifier(Integer id, UtilisateurRequest requete) {
        exigerRequete(requete);

        Utilisateur utilisateur = trouverUtilisateur(id);
        String email = verifierEmail(requete.email());
        Role role = trouverRole(requete.role());

        verifierEmailDisponible(email, id);

        if (estUtilisateurCourant(id) && !ROLE_ADMIN.equalsIgnoreCase(role.getNom())) {
            throw erreur(
                    HttpStatus.CONFLICT,
                    "Vous ne pouvez pas retirer votre propre rôle administrateur."
            );
        }

        String ancienEmail = utilisateur.getEmail();

        utilisateur.setNom(verifierNom(requete.nom()));
        utilisateur.setPrenom(nettoyer(requete.prenom()));
        utilisateur.setEmail(email);
        utilisateur.setRole(role);

        if (requete.motDePasse() != null && !requete.motDePasse().isEmpty()) {
            utilisateur.setMotDePasse(encoder.encode(verifierMotDePasse(requete.motDePasse())));
        }

        Utilisateur enregistre = utilisateurs.save(utilisateur);

        auditer(
                "MODIFICATION_UTILISATEUR",
                "Modification de l'utilisateur : " + ancienEmail + " → " + enregistre.getEmail(),
                enregistre
        );

        return UtilisateurDTO.depuis(enregistre);
    }

    @Transactional
    public UtilisateurDTO changerStatut(Integer id, boolean actif) {
        Utilisateur utilisateur = trouverUtilisateur(id);

        if (Boolean.TRUE.equals(utilisateur.getActif()) == actif) {
            return UtilisateurDTO.depuis(utilisateur);
        }

        if (!actif && estUtilisateurCourant(id)) {
            throw erreur(HttpStatus.CONFLICT, "Vous ne pouvez pas désactiver votre propre compte.");
        }

        utilisateur.setActif(actif);
        Utilisateur enregistre = utilisateurs.save(utilisateur);

        auditer(
                actif ? "ACTIVATION_UTILISATEUR" : "DESACTIVATION_UTILISATEUR",
                (actif ? "Activation du compte : " : "Désactivation du compte : ")
                        + enregistre.getEmail(),
                enregistre
        );

        return UtilisateurDTO.depuis(enregistre);
    }

    private void auditer(String action, String description, Utilisateur cible) {
        journalAuditService.enregistrer(
                action, MODULE, description, ENTITE,
                String.valueOf(cible.getId()), JournalAuditService.SUCCES
        );
    }

    private static boolean estUtilisateurCourant(Integer id) {
        return UtilisateurCourant.id().map(id::equals).orElse(false);
    }

    private Utilisateur trouverUtilisateur(Integer id) {
        return utilisateurs.findById(id)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }

    private Role trouverRole(String nomRole) {
        if (nomRole == null || !ROLES.contains(nomRole.trim().toUpperCase(Locale.ROOT))) {
            throw erreur(HttpStatus.BAD_REQUEST, "Rôle invalide.");
        }

        return roles.findByNomIgnoreCase(nomRole.trim())
                .orElseThrow(() -> erreur(HttpStatus.BAD_REQUEST, "Rôle inexistant."));
    }

    private static void exigerRequete(UtilisateurRequest requete) {
        if (requete == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Requête invalide.");
        }
    }

    private static String verifierNom(String nom) {
        if (nom == null || nom.isBlank()) {
            throw erreur(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        return nom.trim();
    }

    private static String verifierEmail(String email) {
        if (email == null || email.isBlank()) {
            throw erreur(HttpStatus.BAD_REQUEST, "L'email est obligatoire.");
        }

        String valeur = email.trim().toLowerCase(Locale.ROOT);

        if (!FORMAT_EMAIL.matcher(valeur).matches()) {
            throw erreur(HttpStatus.BAD_REQUEST, "Adresse email invalide.");
        }

        return valeur;
    }

    private static String verifierMotDePasse(String motDePasse) {
        if (motDePasse == null || motDePasse.isBlank()) {
            throw erreur(HttpStatus.BAD_REQUEST, "Veuillez renseigner le mot de passe.");
        }

        if (motDePasse.length() < LONGUEUR_MIN_MOT_DE_PASSE) {
            throw erreur(
                    HttpStatus.BAD_REQUEST,
                    "Le mot de passe doit contenir au moins "
                            + LONGUEUR_MIN_MOT_DE_PASSE + " caractères."
            );
        }

        return motDePasse;
    }

    private void verifierEmailDisponible(String email, Integer idActuel) {
        utilisateurs.findByEmailIgnoreCase(email).ifPresent(existant -> {
            if (!existant.getId().equals(idActuel)) {
                throw erreur(HttpStatus.CONFLICT, "Cet email est déjà utilisé.");
            }
        });
    }

    private static String nettoyer(String valeur) {
        return valeur == null ? "" : valeur.trim();
    }

    private static ResponseStatusException erreur(HttpStatus statut, String message) {
        return new ResponseStatusException(statut, message);
    }
}
