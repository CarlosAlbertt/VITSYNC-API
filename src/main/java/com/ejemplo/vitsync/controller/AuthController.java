package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173") // Permitir peticiones desde Vue.js
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Endpoint para iniciar sesión
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message("Credenciales inválidas: " + e.getMessage())
                            .build());
        }
    }

    /**
     * POST /api/auth/register
     * Endpoint para registrar un nuevo usuario
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AuthResponse.builder()
                            .message("Error al registrar usuario: " + e.getMessage())
                            .build());
        }
    }

    /**
     * GET /api/auth/test
     * Endpoint de prueba (no requiere autenticación)
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("API de autenticación funcionando correctamente");
    }
}