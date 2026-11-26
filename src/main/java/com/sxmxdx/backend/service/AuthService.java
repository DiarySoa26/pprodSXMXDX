package com.sxmxdx.backend.service;

import com.sxmxdx.backend.dto.LoginResponse;
import com.sxmxdx.backend.entity.JournalConnexion;
import com.sxmxdx.backend.entity.Utilisateur;
import com.sxmxdx.backend.exception.EchecConnexionException;
import com.sxmxdx.backend.repository.JournalConnexionRepository;
import com.sxmxdx.backend.repository.UtilisateurRepository;
import com.sxmxdx.backend.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private static final String IDENTIFIANTS_INCORRECTS = "Email ou mot de passe incorrect.";

    private final UtilisateurRepository utilisateurRepository;
    private final JournalConnexionRepository journalConnexionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Empreinte factice comparée quand l'email est inconnu, pour que le temps
     * de réponse ne révèle pas l'existence d'un compte.
     */
    private final String empreinteFactice;

    public AuthService(
            UtilisateurRepository utilisateurRepository,
            JournalConnexionRepository journalConnexionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.utilisateurRepository = utilisateurRepository;
        this.journalConnexionRepository = journalConnexionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.empreinteFactice = passwordEncoder.encode("mot-de-passe-factice");
    }

    /**
     * @throws EchecConnexionException si la connexion est refusée
     */
    public LoginResponse login(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new EchecConnexionException(
                    HttpStatus.BAD_REQUEST, "Email et mot de passe obligatoires."
            );
        }

        Optional<Utilisateur> resultat =
                utilisateurRepository.findByEmailIgnoreCase(email.trim());

        if (resultat.isEmpty()) {
            passwordEncoder.matches(password, empreinteFactice);
            throw new EchecConnexionException(HttpStatus.UNAUTHORIZED, IDENTIFIANTS_INCORRECTS);
        }

        Utilisateur utilisateur = resultat.get();

        if (!passwordEncoder.matches(password, utilisateur.getMotDePasse())) {
            enregistrerConnexion(utilisateur, false);
            throw new EchecConnexionException(HttpStatus.UNAUTHORIZED, IDENTIFIANTS_INCORRECTS);
        }

        if (!Boolean.TRUE.equals(utilisateur.getActif())) {
            enregistrerConnexion(utilisateur, false);
            throw new EchecConnexionException(HttpStatus.FORBIDDEN, "Ce compte est désactivé.");
        }

        utilisateur.setDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
        enregistrerConnexion(utilisateur, true);

        return new LoginResponse(
                true,
                "Connexion réussie.",
                utilisateur.getId(),
                nomComplet(utilisateur),
                utilisateur.getEmail(),
                utilisateur.getRole().getNom(),
                jwtService.genererJeton(utilisateur),
                jwtService.dureeValiditeSecondes()
        );
    }

    private static String nomComplet(Utilisateur utilisateur) {
        String prenom = utilisateur.getPrenom() == null ? "" : utilisateur.getPrenom();
        return (prenom + " " + utilisateur.getNom()).trim();
    }

    private void enregistrerConnexion(Utilisateur utilisateur, boolean succes) {
        JournalConnexion journal = new JournalConnexion();
        journal.setUtilisateur(utilisateur);
        journal.setSucces(succes);
        journal.setDateConnexion(LocalDateTime.now());
        journalConnexionRepository.save(journal);
    }
}
