package com.sxmxdx.backend.controller.campagne;

import com.sxmxdx.backend.dto.campagne.CampagneApresBudgetRequest;
import com.sxmxdx.backend.dto.campagne.CampagneRequest;
import com.sxmxdx.backend.dto.campagne.JalonRequest;
import com.sxmxdx.backend.entity.campagne.CampagneBudgetaire;
import com.sxmxdx.backend.entity.campagne.JalonBudgetaire;
import com.sxmxdx.backend.service.campagne.CampagneBudgetaireService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campagnes")
public class CampagneBudgetaireController {

    private final CampagneBudgetaireService service;

    public CampagneBudgetaireController(CampagneBudgetaireService service) {
        this.service = service;
    }

    @GetMapping
    public List<CampagneBudgetaire> lister() {
        return service.lister();
    }

    @GetMapping("/{id}")
    public Map<String, Object> details(@PathVariable Long id) {
        return service.details(id);
    }

    @PostMapping
    public CampagneBudgetaire creer(@RequestBody CampagneRequest requete) {
        return service.creer(requete);
    }

    @PostMapping("/apres-generation")
    public CampagneBudgetaire creerApresBudget(@RequestBody CampagneApresBudgetRequest requete) {
        return service.creerApresBudget(requete);
    }

    @PutMapping("/{id}")
    public CampagneBudgetaire modifier(@PathVariable Long id, @RequestBody CampagneRequest requete) {
        return service.modifier(id, requete);
    }

    @PatchMapping("/{id}/jalons/{jalonId}")
    public JalonBudgetaire modifierJalon(
            @PathVariable Long id,
            @PathVariable Long jalonId,
            @RequestBody JalonRequest requete
    ) {
        return service.modifierJalon(id, jalonId, requete);
    }

    @PatchMapping("/{id}/jalons/{jalonId}/terminer")
    public JalonBudgetaire terminerJalon(@PathVariable Long id, @PathVariable Long jalonId) {
        return service.terminerJalon(id, jalonId);
    }

    @PatchMapping("/{id}/valider")
    public CampagneBudgetaire valider(@PathVariable Long id) {
        return service.validerCampagne(id);
    }
}
