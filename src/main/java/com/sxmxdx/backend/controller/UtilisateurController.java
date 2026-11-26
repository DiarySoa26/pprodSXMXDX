package com.sxmxdx.backend.controller;

import com.sxmxdx.backend.dto.UtilisateurDTO;
import com.sxmxdx.backend.dto.UtilisateurRequest;
import com.sxmxdx.backend.service.UtilisateurService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService service;

    public UtilisateurController(UtilisateurService service) {
        this.service = service;
    }

    @GetMapping
    public List<UtilisateurDTO> lister() {
        return service.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDTO creer(@RequestBody UtilisateurRequest request) {
        return service.creer(request);
    }

    @PutMapping("/{id}")
    public UtilisateurDTO modifier(
            @PathVariable Integer id,
            @RequestBody UtilisateurRequest request
    ) {
        return service.modifier(id, request);
    }

    @PatchMapping("/{id}/statut")
    public UtilisateurDTO changerStatut(
            @PathVariable Integer id,
            @RequestBody Map<String, Boolean> request
    ) {
        Boolean actif = request == null ? null : request.get("actif");

        if (actif == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le champ 'actif' est obligatoire.");
        }

        return service.changerStatut(id, actif);
    }
}
