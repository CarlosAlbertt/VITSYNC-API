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
@CrossOrigin(origins = { "https://vitsync-web-app.vercel.app", "http://localhost:5173", "http://localhost:4200" })
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    // GET /api/auth/validate - Validar token (opcional, útil para el frontend)
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok().body("{\"valid\": true}");
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"valid\": false}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("{\"valid\": false}");
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody VerifyRequest request){
        try{
            authService.verifyAccount(request.getEmail(), request.getCode());
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Cuenta verificada exitosamente")
                    .build());
        }catch (RuntimeException e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AuthResponse.builder()
                            .message(e.getMessage())
                            .build());
        }
    }
}
