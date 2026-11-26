package com.sxmxdx.backend.service;

import com.sxmxdx.backend.entity.JournalAudit;
import com.sxmxdx.backend.repository.JournalAuditRepository;
import com.sxmxdx.backend.security.UtilisateurCourant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Piste d'audit des actions métier.
 * <p>
 * L'auteur est l'utilisateur authentifié de la requête. Chaque entrée est
 * écrite dans sa propre transaction : elle survit à l'annulation de l'action
 * tracée, et une panne du journal ne fait jamais échouer l'action elle-même.
 */
@Service
public class JournalAuditService {

    public static final String SUCCES = "SUCCES";
    public static final String ECHEC = "ECHEC";

    private static final Logger log = LoggerFactory.getLogger(JournalAuditService.class);

    private final JournalAuditRepository repository;
    private final TransactionTemplate nouvelleTransaction;

    public JournalAuditService(
            JournalAuditRepository repository,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.nouvelleTransaction = new TransactionTemplate(transactionManager);
        this.nouvelleTransaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );
    }

    public void enregistrer(
            String action,
            String module,
            String description,
            String entiteType,
            String entiteId,
            String statut
    ) {
        JournalAudit journal = new JournalAudit();
        journal.setUtilisateurId(UtilisateurCourant.id().orElse(null));
        journal.setAction(action);
        journal.setModule(module);
        journal.setDescription(description);
        journal.setEntiteType(entiteType);
        journal.setEntiteId(entiteId);
        journal.setStatut(statut);

        try {
            nouvelleTransaction.executeWithoutResult(etat -> repository.save(journal));
        } catch (RuntimeException e) {
            log.error("Impossible d'enregistrer l'audit {} : {}", action, e.getMessage());
        }
    }
}
