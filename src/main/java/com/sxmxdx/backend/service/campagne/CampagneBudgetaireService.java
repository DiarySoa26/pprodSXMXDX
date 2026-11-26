package com.sxmxdx.backend.service.campagne;

import com.sxmxdx.backend.dto.campagne.CampagneApresBudgetRequest;
import com.sxmxdx.backend.dto.campagne.CampagneRequest;
import com.sxmxdx.backend.dto.campagne.JalonRequest;
import com.sxmxdx.backend.entity.budget.Budget;
import com.sxmxdx.backend.entity.campagne.CampagneBudgetaire;
import com.sxmxdx.backend.entity.campagne.JalonBudgetaire;
import com.sxmxdx.backend.repository.budget.BudgetRepository;
import com.sxmxdx.backend.repository.campagne.CampagneBudgetaireRepository;
import com.sxmxdx.backend.repository.campagne.JalonBudgetaireRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceRepository;
import com.sxmxdx.backend.repository.exercice.ExerciceViewRepository;
import com.sxmxdx.backend.service.JournalAuditService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.sxmxdx.backend.entity.campagne.JalonBudgetaire.GENERATION;
import static com.sxmxdx.backend.entity.campagne.JalonBudgetaire.IMPORTATION;
import static com.sxmxdx.backend.entity.campagne.JalonBudgetaire.VALIDATION;
import static com.sxmxdx.backend.entity.campagne.JalonBudgetaire.VERIFICATION;

/**
 * Campagnes budgétaires : un exercice suivi à travers quatre jalons
 * (importation, génération, vérification, validation).
 */
@Service
public class CampagneBudgetaireService {

    private static final String MODULE = "CAMPAGNE";
    private static final String ENTITE_CAMPAGNE = "CAMPAGNE";
    private static final String ENTITE_JALON = "JALON";

    /** Jalons dont l'échéance peut être modifiée à la main. */
    private static final Set<String> JALONS_MANUELS = Set.of(VERIFICATION, VALIDATION);

    private final CampagneBudgetaireRepository campagnes;
    private final JalonBudgetaireRepository jalons;
    private final ExerciceRepository exercices;
    private final ExerciceViewRepository vuesExercice;
    private final BudgetRepository budgets;
    private final JournalAuditService journalAuditService;

    public CampagneBudgetaireService(
            CampagneBudgetaireRepository campagnes,
            JalonBudgetaireRepository jalons,
            ExerciceRepository exercices,
            ExerciceViewRepository vuesExercice,
            BudgetRepository budgets,
            JournalAuditService journalAuditService
    ) {
        this.campagnes = campagnes;
        this.jalons = jalons;
        this.exercices = exercices;
        this.vuesExercice = vuesExercice;
        this.budgets = budgets;
        this.journalAuditService = journalAuditService;
    }

    // ------------------------------------------------------------------
    // Lecture
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<CampagneBudgetaire> lister() {
        return campagnes.findAllByOrderByDateDebutDesc();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> details(Long id) {
        CampagneBudgetaire campagne = trouverCampagne(id);
        List<JalonBudgetaire> liste = jalons.findByCampagneIdOrderByOrdreAsc(id);

        long termines = liste.stream().filter(JalonBudgetaire::estTermine).count();

        long retards = liste.stream()
                .filter(jalon -> !jalon.estTermine())
                .filter(jalon -> jalon.getDateLimite().isBefore(LocalDate.now()))
                .count();

        double progression = liste.isEmpty()
                ? 0
                : Math.round(10000.0 * termines / liste.size()) / 100.0;

        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("campagne", campagne);
        resultat.put("jalons", liste);
        resultat.put("totalJalons", liste.size());
        resultat.put("jalonsTermines", termines);
        resultat.put("jalonsEnRetard", retards);
        resultat.put("progression", progression);
        return resultat;
    }

    // ------------------------------------------------------------------
    // Création
    // ------------------------------------------------------------------

    @Transactional
    public CampagneBudgetaire creer(CampagneRequest r) {
        return tracerCampagne(
                "CREATION_CAMPAGNE",
                r == null ? null : r.exerciceId(),
                () -> {
                    verifier(r);

                    CampagneBudgetaire campagne = new CampagneBudgetaire();
                    campagne.setNom(r.nom().trim());
                    campagne.setExerciceId(r.exerciceId());
                    campagne.setDateDebut(r.dateDebut());
                    campagne.setDateFin(r.dateFin());
                    campagne.setStatut(CampagneBudgetaire.PLANIFIEE);
                    campagne = campagnes.save(campagne);

                    Long id = campagne.getId();
                    jalons.saveAll(List.of(
                            jalon(id, "Importation", IMPORTATION, 1, r.dateImportation()),
                            jalon(id, "Génération du budget", GENERATION, 2, r.dateGeneration()),
                            jalon(id, "Vérification", VERIFICATION, 3, r.dateVerification()),
                            jalon(id, "Validation", VALIDATION, 4, r.dateValidation())
                    ));

                    return campagne;
                },
                campagne -> "Création de la campagne " + campagne.getNom()
        );
    }

    /**
     * Crée la campagne d'un exercice dont le budget est déjà généré : les
     * jalons d'importation et de génération sont repris de l'historique.
     */
    @Transactional
    public CampagneBudgetaire creerApresBudget(CampagneApresBudgetRequest r) {
        return tracerCampagne(
                "CREATION_CAMPAGNE_APRES_BUDGET",
                r == null ? null : r.exerciceId(),
                () -> {
                    if (r == null || r.exerciceId() == null || r.anneeExercice() == null
                            || r.dateVerification() == null || r.dateValidation() == null) {
                        throw erreur(HttpStatus.BAD_REQUEST, "Exercice et échéances obligatoires");
                    }

                    if (!exercices.existsByIdAndAnnee(r.exerciceId(), r.anneeExercice())) {
                        throw erreur(HttpStatus.BAD_REQUEST, "Exercice introuvable ou année incohérente");
                    }

                    if (campagnes.existsByExerciceId(r.exerciceId())) {
                        throw erreur(HttpStatus.CONFLICT, "Une campagne existe déjà pour cet exercice");
                    }

                    Budget budget = dernierBudgetGenere(r.exerciceId());
                    LocalDateTime dateGeneration = budget.getDateGeneration();
                    LocalDateTime dateImportation = dateImportation(budget, r.exerciceId());

                    if (dateImportation.isAfter(dateGeneration)) {
                        throw erreur(HttpStatus.CONFLICT, "L'importation est postérieure à la génération");
                    }

                    if (r.dateVerification().isBefore(dateGeneration.toLocalDate())) {
                        throw erreur(HttpStatus.BAD_REQUEST, "La vérification doit suivre la génération");
                    }

                    if (r.dateValidation().isBefore(r.dateVerification())) {
                        throw erreur(HttpStatus.BAD_REQUEST, "La validation doit suivre la vérification");
                    }

                    CampagneBudgetaire campagne = new CampagneBudgetaire();
                    campagne.setNom("Campagne " + r.anneeExercice());
                    campagne.setExerciceId(r.exerciceId());
                    campagne.setDateDebut(dateImportation.toLocalDate());
                    campagne.setDateFin(r.dateValidation());
                    campagne.setStatut(CampagneBudgetaire.EN_COURS);
                    campagne = campagnes.save(campagne);

                    Long id = campagne.getId();
                    jalons.saveAll(List.of(
                            jalonTermine(id, "Importation", IMPORTATION, 1, dateImportation),
                            jalonTermine(id, "Génération du budget", GENERATION, 2, dateGeneration),
                            jalon(id, "Vérification", VERIFICATION, 3, r.dateVerification()),
                            jalon(id, "Validation", VALIDATION, 4, r.dateValidation())
                    ));

                    return campagne;
                },
                campagne -> "Création après génération : " + campagne.getNom()
        );
    }

    // ------------------------------------------------------------------
    // Modification
    // ------------------------------------------------------------------

    @Transactional
    public CampagneBudgetaire modifier(Long id, CampagneRequest r) {
        return tracerCampagne(
                "MODIFICATION_CAMPAGNE", id,
                () -> {
                    verifier(r);

                    CampagneBudgetaire campagne = trouverCampagneModifiable(id);

                    if (!campagne.getExerciceId().equals(r.exerciceId())) {
                        throw erreur(HttpStatus.CONFLICT, "L'exercice ne peut pas être modifié");
                    }

                    List<JalonBudgetaire> liste = jalons.findByCampagneIdOrderByOrdreAsc(id);

                    if (liste.stream().anyMatch(JalonBudgetaire::estTermine)) {
                        throw erreur(
                                HttpStatus.CONFLICT,
                                "Utilisez la modification individuelle des échéances"
                        );
                    }

                    campagne.setNom(r.nom().trim());
                    campagne.setDateDebut(r.dateDebut());
                    campagne.setDateFin(r.dateFin());

                    Map<String, LocalDate> echeances = Map.of(
                            IMPORTATION, r.dateImportation(),
                            GENERATION, r.dateGeneration(),
                            VERIFICATION, r.dateVerification(),
                            VALIDATION, r.dateValidation()
                    );

                    liste.forEach(jalon -> {
                        LocalDate echeance = echeances.get(jalon.getCode());
                        if (echeance != null) {
                            jalon.setDateLimite(echeance);
                        }
                    });
                    jalons.saveAll(liste);

                    return campagnes.save(campagne);
                },
                campagne -> "Modification de la campagne " + id
        );
    }

    @Transactional
    public JalonBudgetaire modifierJalon(Long campagneId, Long jalonId, JalonRequest r) {
        return tracerJalon(
                "MODIFICATION_JALON", jalonId,
                () -> {
                    CampagneBudgetaire campagne = trouverCampagneModifiable(campagneId);
                    JalonBudgetaire jalon = trouverJalon(campagneId, jalonId);

                    if (!JALONS_MANUELS.contains(jalon.getCode())) {
                        throw erreur(HttpStatus.CONFLICT, "L'échéance de ce jalon est automatique");
                    }

                    if (jalon.estTermine()) {
                        throw erreur(HttpStatus.CONFLICT, "Impossible de modifier un jalon terminé");
                    }

                    if (r == null || r.dateLimite() == null) {
                        throw erreur(HttpStatus.BAD_REQUEST, "Date limite obligatoire");
                    }

                    LocalDate nouvelleDate = r.dateLimite();

                    if (nouvelleDate.isBefore(campagne.getDateDebut())) {
                        throw erreur(HttpStatus.BAD_REQUEST, "Date antérieure au début de la campagne");
                    }

                    verifierOrdreEcheances(campagneId, jalon, nouvelleDate);

                    jalon.setDateLimite(nouvelleDate);

                    if (VALIDATION.equals(jalon.getCode())) {
                        campagne.setDateFin(nouvelleDate);
                        campagnes.save(campagne);
                    }

                    return jalons.save(jalon);
                },
                jalon -> "Modification du jalon " + jalonId + " de la campagne " + campagneId
        );
    }

    // ------------------------------------------------------------------
    // Avancement
    // ------------------------------------------------------------------

    /** Confirme la vérification du budget (jalon VERIFICATION). */
    @Transactional
    public JalonBudgetaire terminerJalon(Long campagneId, Long jalonId) {
        return tracerJalon(
                "VERIFICATION_BUDGET", jalonId,
                () -> {
                    trouverCampagneModifiable(campagneId);
                    JalonBudgetaire jalon = trouverJalon(campagneId, jalonId);

                    if (!VERIFICATION.equals(jalon.getCode())) {
                        throw erreur(HttpStatus.CONFLICT, "Seule la vérification est confirmée ici");
                    }

                    exigerTermine(campagneId, IMPORTATION);
                    exigerTermine(campagneId, GENERATION);

                    jalon.terminer();
                    return jalons.save(jalon);
                },
                jalon -> "Vérification du jalon " + jalonId + " de la campagne " + campagneId
        );
    }

    /** Validation finale : clôt la campagne une fois les jalons précédents terminés. */
    @Transactional
    public CampagneBudgetaire validerCampagne(Long campagneId) {
        return tracerCampagne(
                "VALIDATION_CAMPAGNE", campagneId,
                () -> {
                    CampagneBudgetaire campagne = trouverCampagneModifiable(campagneId);

                    exigerTermine(campagneId, IMPORTATION);
                    exigerTermine(campagneId, GENERATION);
                    exigerTermine(campagneId, VERIFICATION);

                    JalonBudgetaire validation = jalons.findByCampagneIdAndCode(campagneId, VALIDATION)
                            .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "Jalon VALIDATION introuvable"));

                    validation.terminer();
                    jalons.save(validation);

                    campagne.setStatut(CampagneBudgetaire.TERMINEE);
                    return campagnes.save(campagne);
                },
                campagne -> "Validation finale de la campagne " + campagne.getNom()
        );
    }

    // ------------------------------------------------------------------
    // Règles
    // ------------------------------------------------------------------

    private static void verifier(CampagneRequest r) {
        if (r == null || r.nom() == null || r.nom().isBlank()
                || r.exerciceId() == null
                || r.dateDebut() == null
                || r.dateFin() == null
                || r.dateImportation() == null
                || r.dateGeneration() == null
                || r.dateVerification() == null
                || r.dateValidation() == null) {
            throw erreur(HttpStatus.BAD_REQUEST, "Champs obligatoires manquants");
        }

        List<LocalDate> chronologie = List.of(
                r.dateDebut(),
                r.dateImportation(),
                r.dateGeneration(),
                r.dateVerification(),
                r.dateValidation(),
                r.dateFin()
        );

        for (int i = 1; i < chronologie.size(); i++) {
            if (chronologie.get(i).isBefore(chronologie.get(i - 1))) {
                throw erreur(HttpStatus.BAD_REQUEST, "Ordre chronologique des échéances invalide");
            }
        }
    }

    private void verifierOrdreEcheances(Long campagneId, JalonBudgetaire jalon, LocalDate nouvelleDate) {
        for (JalonBudgetaire autre : jalons.findByCampagneIdOrderByOrdreAsc(campagneId)) {
            if (autre.getId().equals(jalon.getId())) {
                continue;
            }

            if (autre.getOrdre() < jalon.getOrdre() && autre.getDateLimite().isAfter(nouvelleDate)) {
                throw erreur(HttpStatus.BAD_REQUEST, "Échéance avant un jalon précédent");
            }

            if (autre.getOrdre() > jalon.getOrdre() && autre.getDateLimite().isBefore(nouvelleDate)) {
                throw erreur(HttpStatus.BAD_REQUEST, "Échéance après un jalon suivant");
            }
        }
    }

    private void exigerTermine(Long campagneId, String code) {
        JalonBudgetaire jalon = jalons.findByCampagneIdAndCode(campagneId, code)
                .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "Jalon absent : " + code));

        if (!jalon.estTermine()) {
            throw erreur(HttpStatus.CONFLICT, "Terminer d'abord le jalon : " + code);
        }
    }

    private Budget dernierBudgetGenere(Long exerciceId) {
        Budget budget = budgets.findGeneresParDateGeneration(exerciceId).stream()
                .findFirst()
                .orElseThrow(() -> erreur(
                        HttpStatus.CONFLICT, "Générer d'abord le budget de cet exercice"));

        if (budget.getDateGeneration() == null) {
            throw erreur(HttpStatus.CONFLICT, "Date de génération du budget indisponible");
        }

        if (budget.getImportId() == null) {
            throw erreur(HttpStatus.CONFLICT, "Le budget ne possède pas d'importation associée");
        }

        return budget;
    }

    private LocalDateTime dateImportation(Budget budget, Long exerciceId) {
        return vuesExercice
                .findDateImport(budget.getImportId().intValue(), exerciceId.intValue())
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> erreur(HttpStatus.CONFLICT, "Date d'importation introuvable"));
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private CampagneBudgetaire trouverCampagne(Long id) {
        return campagnes.findById(id)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Campagne introuvable"));
    }

    private CampagneBudgetaire trouverCampagneModifiable(Long id) {
        CampagneBudgetaire campagne = trouverCampagne(id);

        if (campagne.estFermee()) {
            throw erreur(HttpStatus.CONFLICT, "Campagne fermée");
        }

        return campagne;
    }

    private JalonBudgetaire trouverJalon(Long campagneId, Long jalonId) {
        JalonBudgetaire jalon = jalons.findById(jalonId)
                .orElseThrow(() -> erreur(HttpStatus.NOT_FOUND, "Jalon introuvable"));

        if (!campagneId.equals(jalon.getCampagneId())) {
            throw erreur(HttpStatus.NOT_FOUND, "Jalon hors campagne");
        }

        return jalon;
    }

    private static JalonBudgetaire jalon(
            Long campagneId, String nom, String code, int ordre, LocalDate dateLimite
    ) {
        JalonBudgetaire jalon = new JalonBudgetaire();
        jalon.setCampagneId(campagneId);
        jalon.setNom(nom);
        jalon.setCode(code);
        jalon.setOrdre(ordre);
        jalon.setDateLimite(dateLimite);
        jalon.setStatut(JalonBudgetaire.EN_ATTENTE);
        return jalon;
    }

    private static JalonBudgetaire jalonTermine(
            Long campagneId, String nom, String code, int ordre, LocalDateTime dateRealisation
    ) {
        JalonBudgetaire jalon = jalon(campagneId, nom, code, ordre, dateRealisation.toLocalDate());
        jalon.setDateRealisation(dateRealisation);
        jalon.setStatut(JalonBudgetaire.TERMINE);
        return jalon;
    }

    private CampagneBudgetaire tracerCampagne(
            String action,
            Long idConnu,
            Supplier<CampagneBudgetaire> traitement,
            Function<CampagneBudgetaire, String> description
    ) {
        return tracer(action, ENTITE_CAMPAGNE, idConnu, CampagneBudgetaire::getId, traitement, description);
    }

    private JalonBudgetaire tracerJalon(
            String action,
            Long idConnu,
            Supplier<JalonBudgetaire> traitement,
            Function<JalonBudgetaire, String> description
    ) {
        return tracer(action, ENTITE_JALON, idConnu, JalonBudgetaire::getId, traitement, description);
    }

    /**
     * Exécute une action et la trace : le succès est journalisé après la
     * validation de la transaction, l'échec (préfixé ECHEC_) immédiatement.
     *
     * @param idConnu identifiant tracé en cas d'échec (peut être {@code null})
     */
    private <T> T tracer(
            String action,
            String entiteType,
            Long idConnu,
            Function<T, Long> identifiant,
            Supplier<T> traitement,
            Function<T, String> description
    ) {
        try {
            T resultat = traitement.get();
            apresValidation(() -> journalAuditService.enregistrer(
                    action, MODULE, description.apply(resultat), entiteType,
                    texte(identifiant.apply(resultat)), JournalAuditService.SUCCES
            ));
            return resultat;

        } catch (RuntimeException e) {
            journalAuditService.enregistrer(
                    "ECHEC_" + action, MODULE, "Échec : " + e.getMessage(), entiteType,
                    texte(idConnu), JournalAuditService.ECHEC
            );
            throw e;
        }
    }

    private static String texte(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private static void apresValidation(Runnable tache) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    tache.run();
                }
            });
        } else {
            tache.run();
        }
    }

    private static ResponseStatusException erreur(HttpStatus statut, String message) {
        return new ResponseStatusException(statut, message);
    }
}
