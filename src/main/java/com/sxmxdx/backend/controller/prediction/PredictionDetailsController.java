package com.sxmxdx.backend.controller.prediction;

import com.sxmxdx.backend.service.prediction.PredictionDetailsService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/predictions/details")
public class PredictionDetailsController {

    private final PredictionDetailsService service;

    public PredictionDetailsController(PredictionDetailsService service) {
        this.service = service;
    }

    @GetMapping("/{annee}/amortissements")
    public Map<String, Object> amortissements(@PathVariable Integer annee) {
        return service.getAmortissements(annee);
    }

    @GetMapping("/{annee}/charges-categories")
    public Map<String, Object> chargesCategories(@PathVariable Integer annee) {
        return service.getChargesCategories(annee);
    }
}
