package com.sxmxdx.backend.controller.prediction;

import com.sxmxdx.backend.dto.prediction.PredictionDTO;
import com.sxmxdx.backend.dto.prediction.PredictionResumeDTO;
import com.sxmxdx.backend.service.prediction.PredictionService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionService predictionService;

    public PredictionController(PredictionService predictionService) {
        this.predictionService = predictionService;
    }

    @PostMapping("/generer")
    public Map<?, ?> genererPrediction() {
        return predictionService.genererPrediction();
    }

    @GetMapping("/last")
    public ResponseEntity<PredictionDTO> dernierePrediction() {
        return ResponseEntity.of(predictionService.dernierePrediction());
    }

    @GetMapping
    public List<PredictionResumeDTO> listerPredictions() {
        return predictionService.listerPredictions();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PredictionDTO> rechercherPrediction(@PathVariable Long id) {
        return ResponseEntity.of(predictionService.rechercherPrediction(id));
    }
}
