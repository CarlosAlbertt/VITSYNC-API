package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.dto.VerifyRequest;
import com.ejemplo.vitsync.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")

public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final com.ejemplo.vitsync.util.JwtUtil jwtUtil;

    public AuthController(AuthService authService, com.ejemplo.vitsync.util.JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    // POST /api/auth/login - Iniciar sesión
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            logger.info("Intento de login para usuario: {}", request.getNif());
            AuthResponse response = authService.login(request);
            logger.info("Login exitoso para usuario: {}", request.getNif());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error en login: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    // POST /api/auth/register - Registrar nuevo usuario
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            logger.info("Intento de registro para NIF: {}", request.getNif());
            AuthResponse response = authService.register(request);
            logger.info("Registro exitoso para NIF: {}", request.getNif());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Error en registro: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    // GET /api/auth/validate - Validar token JWT (decodifica y verifica firma + expiración)
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"valid\": false, \"error\": \"Token no proporcionado\"}");
            }

            String token = authHeader.substring(7);
            String nif = jwtUtil.extractNif(token);

            // Validar firma, expiración y que el usuario existe
            if (nif != null && jwtUtil.validateToken(token, nif)) {
                String role = jwtUtil.extractRole(token);
                return ResponseEntity.ok()
                        .body("{\"valid\": true, \"nif\": \"" + nif + "\", \"role\": \"" + role + "\"}");
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"valid\": false, \"error\": \"Token inválido o expirado\"}");
        } catch (Exception e) {
            logger.warn("Error validando token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("{\"valid\": false, \"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request) {
        try {
            authService.verifyAccount(request.getEmail(), request.getCode());
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Cuenta verificada exitosamente")
                    .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }
}
