package com.sxmxdx.backend.service;

import com.sxmxdx.backend.entity.ViewJournalAudit;
import com.sxmxdx.backend.entity.ViewJournalConnexion;
import com.sxmxdx.backend.repository.ViewJournalAuditRepository;
import com.sxmxdx.backend.repository.ViewJournalConnexionRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TracabiliteService {

    private final ViewJournalAuditRepository actions;
    private final ViewJournalConnexionRepository connexions;

    public TracabiliteService(
            ViewJournalAuditRepository actions,
            ViewJournalConnexionRepository connexions
    ) {
        this.actions = actions;
        this.connexions = connexions;
    }

    public List<ViewJournalAudit> listerActions() {
        return actions.findAllByOrderByDateActionDesc();
    }

    public List<ViewJournalConnexion> listerConnexions() {
        return connexions.findAllByOrderByDateConnexionDesc();
    }
}
