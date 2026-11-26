package com.sxmxdx.backend.controller;

import com.sxmxdx.backend.entity.ViewJournalAudit;
import com.sxmxdx.backend.entity.ViewJournalConnexion;
import com.sxmxdx.backend.service.TracabiliteService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tracabilite")
public class TracabiliteController {

    private final TracabiliteService service;

    public TracabiliteController(TracabiliteService service) {
        this.service = service;
    }

    @GetMapping("/actions")
    public List<ViewJournalAudit> actions() {
        return service.listerActions();
    }

    @GetMapping("/connexions")
    public List<ViewJournalConnexion> connexions() {
        return service.listerConnexions();
    }
}
