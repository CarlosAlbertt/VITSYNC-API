package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RefreshRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.dto.VerifyRequest;
import com.ejemplo.vitsync.service.AuthService;
import com.ejemplo.vitsync.util.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints: login, registration, verification, token
 * refresh and session revocation.
 *
 * <p>Error handling is delegated to {@code GlobalExceptionHandler}: this
 * controller no longer catches generic {@code RuntimeException} (which used
 * to leak internal messages to clients).</p>
 *
 * <p>Used by the frontend as follows: the access token travels in the
 * {@code Authorization: Bearer} header and lives in JS memory; the refresh
 * token MUST be stored in an httpOnly cookie (see README).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user.
     *
     * @param request NIF + password
     * @return 200 with access + refresh tokens; 401 on bad credentials;
     *         400 if the account is unverified/suspended; 429 if rate-limited
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        // Solo se loguea el intento, nunca la contraseña
        logger.info("Intento de login para usuario: {}", request.getNif());
        AuthResponse response = authService.login(request);
        logger.info("Login exitoso para usuario: {}", request.getNif());
        return ResponseEntity.ok(response);
    }

    /**
     * Registers a new account and sends the verification email.
     *
     * @param request validated registration data
     * @return 201 with user summary (no tokens until verification);
     *         400 on validation/duplicate errors; 429 if rate-limited
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        logger.info("Intento de registro para NIF: {}", request.getNif());
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Verifies an account with the emailed 6-digit code.
     *
     * @param request email + code
     * @return 200 on success; 400 on invalid pair; 429 if rate-limited
     */
    @PostMapping("/verify")
    public ResponseEntity<AuthResponse> verify(@Valid @RequestBody VerifyRequest request) {
        authService.verifyAccount(request.getEmail(), request.getCode());
        return ResponseEntity.ok(AuthResponse.builder()
                .message("Cuenta verificada exitosamente")
                .build());
    }

    /**
     * Exchanges a refresh token for a fresh access token (with rotation).
     *
     * @param request the refresh token issued at login or a previous refresh
     * @return 200 with new token pair; 400 if the token is invalid/revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.getRefreshToken()));
    }

    /**
     * Revokes the presented refresh token (single-device logout). Idempotent.
     *
     * @param request the refresh token to revoke
     * @return 200 always (no token-validity oracle)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }

    /**
     * Revokes every active session of the authenticated user.
     *
     * @param authentication injected principal (NIF)
     * @return 200 with the number of sessions revoked; 401 without token
     */
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(Authentication authentication) {
        int revoked = authService.logoutAll(authentication.getName());
        return ResponseEntity.ok(Map.of(
                "message", "Todas las sesiones cerradas",
                "sessionsRevoked", revoked));
    }

    /**
     * Validates an access token (signature + expiry + subject).
     *
     * @param authHeader {@code Authorization: Bearer <token>} header
     * @return 200 with nif/role if valid; 401 otherwise. The response is
     *         built as a Map (Jackson-escaped) — never by string
     *         concatenation, which allowed JSON injection (finding V15)
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "error", "Token no proporcionado"));
        }

        try {
            String token = authHeader.substring(7);
            String nif = jwtUtil.extractNif(token);

            if (nif != null && jwtUtil.validateToken(token, nif)) {
                return ResponseEntity.ok(Map.of(
                        "valid", true,
                        "nif", nif,
                        "role", jwtUtil.extractRole(token)));
            }
        } catch (Exception e) {
            // Detalle solo al log; al cliente, mensaje genérico
            logger.warn("Error validando token: {}", e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", "Token inválido o expirado"));
    }
}
