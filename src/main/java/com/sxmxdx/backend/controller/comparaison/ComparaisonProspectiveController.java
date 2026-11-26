package com.sxmxdx.backend.controller.comparaison;

import com.sxmxdx.backend.dto.comparaison.ComparaisonProspectiveDTO;
import com.sxmxdx.backend.service.comparaison.ComparaisonProspectiveService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/comparaisons/prospective")
public class ComparaisonProspectiveController {

    private final ComparaisonProspectiveService service;

    public ComparaisonProspectiveController(ComparaisonProspectiveService service) {
        this.service = service;
    }

    @GetMapping("/budgets")
    public List<Integer> budgetsDisponibles() {
        return service.getBudgetsDisponibles();
    }

    @GetMapping("/previsions")
    public List<Integer> previsionsDisponibles() {
        return service.getPrevisionsDisponibles();
    }

    @GetMapping
    public ComparaisonProspectiveDTO comparer(
            @RequestParam Integer anneeBudget,
            @RequestParam Integer anneePrevision
    ) {
        return service.comparer(anneeBudget, anneePrevision);
    }
}
