package com.sxmxdx.backend.controller;

import com.sxmxdx.backend.dto.LoginRequest;
import com.sxmxdx.backend.dto.LoginResponse;
import com.sxmxdx.backend.exception.EchecConnexionException;
import com.sxmxdx.backend.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @ExceptionHandler(EchecConnexionException.class)
    public ResponseEntity<LoginResponse> echec(EchecConnexionException e) {
        return ResponseEntity
                .status(e.getStatut())
                .body(LoginResponse.echec(e.getMessage()));
    }
}
