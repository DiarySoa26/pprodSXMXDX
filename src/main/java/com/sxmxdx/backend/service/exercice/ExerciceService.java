package com.sxmxdx.backend.service.exercice;

import com.sxmxdx.backend.dto.exercice.ExerciceDTO;
import com.sxmxdx.backend.repository.exercice.ExerciceViewRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ExerciceService {

    private final ExerciceViewRepository repository;

    public ExerciceService(ExerciceViewRepository repository) {
        this.repository = repository;
    }

    public List<ExerciceDTO> rechercher(Integer annee) {
        return annee == null
                ? repository.findTous()
                : repository.findParAnnee(annee);
    }

    public List<Integer> rechercherAnnees() {
        return repository.findAnnees();
    }
}
