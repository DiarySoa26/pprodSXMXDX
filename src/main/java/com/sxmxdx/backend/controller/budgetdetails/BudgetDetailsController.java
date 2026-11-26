package com.sxmxdx.backend.controller.budgetdetails;

import com.sxmxdx.backend.service.budgetdetails.BudgetDetailsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budget-details")
public class BudgetDetailsController {

    private final BudgetDetailsService service;

    public BudgetDetailsController(BudgetDetailsService service) {
        this.service = service;
    }

    @GetMapping("/charges/{exerciceId}")
    public List<Map<String, Object>> getCharges(@PathVariable Integer exerciceId) {
        return service.getCharges(exerciceId);
    }

    @GetMapping("/exportations/{exerciceId}")
    public List<Map<String, Object>> getExportations(@PathVariable Integer exerciceId) {
        return service.getExportations(exerciceId);
    }

    @GetMapping("/frais-export/{exerciceId}")
    public List<Map<String, Object>> getFraisExport(@PathVariable Integer exerciceId) {
        return service.getFraisExport(exerciceId);
    }

    @GetMapping("/production/{exerciceId}")
    public List<Map<String, Object>> getProduction(@PathVariable Integer exerciceId) {
        return service.getProduction(exerciceId);
    }

    @GetMapping("/employes/{exerciceId}")
    public List<Map<String, Object>> getEmployes(@PathVariable Integer exerciceId) {
        return service.getEmployes(exerciceId);
    }

    @GetMapping("/budget/{exerciceId}")
    public List<Map<String, Object>> getBudget(@PathVariable Integer exerciceId) {
        return service.getBudget(exerciceId);
    }

    @GetMapping("/budget-mensuel/{budgetId}")
    public List<Map<String, Object>> getBudgetMensuel(@PathVariable Integer budgetId) {
        return service.getBudgetMensuel(budgetId);
    }

    @GetMapping("/amortissements/{exerciceId}")
    public List<Map<String, Object>> getAmortissements(@PathVariable Integer exerciceId) {
        return service.getAmortissements(exerciceId);
    }

    @GetMapping("/amortissements-totaux/{exerciceId}")
    public List<Map<String, Object>> getAmortissementsTotaux(@PathVariable Integer exerciceId) {
        return service.getAmortissementsTotaux(exerciceId);
    }

    @GetMapping("/amortissements-annuel/{exerciceId}")
    public List<Map<String, Object>> getAmortissementsAnnuel(@PathVariable Integer exerciceId) {
        return service.getAmortissementsAnnuel(exerciceId);
    }

    @GetMapping("/compte-resultat/{exerciceId}")
    public List<Map<String, Object>> getCompteResultat(@PathVariable Integer exerciceId) {
        return service.getCompteResultat(exerciceId);
    }
}
