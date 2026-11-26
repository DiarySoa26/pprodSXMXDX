package com.sxmxdx.backend.controller.exercice;

import com.sxmxdx.backend.dto.exercice.ExerciceDTO;
import com.sxmxdx.backend.service.exercice.ExerciceService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/exercices")
public class ExerciceController {

    private final ExerciceService service;

    public ExerciceController(ExerciceService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExerciceDTO> rechercher(@RequestParam(required = false) Integer annee) {
        return service.rechercher(annee);
    }

    @GetMapping("/annees")
    public List<Integer> rechercherAnnees() {
        return service.rechercherAnnees();
    }
}
