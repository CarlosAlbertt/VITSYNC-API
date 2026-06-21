package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.AuthResponse;
import com.ejemplo.vitsync.dto.LoginRequest;
import com.ejemplo.vitsync.dto.RefreshRequest;
import com.ejemplo.vitsync.dto.RegisterRequest;
import com.ejemplo.vitsync.dto.SessionResponse;
import com.ejemplo.vitsync.dto.VerifyRequest;
import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.service.AuthService;
import com.ejemplo.vitsync.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
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

    /** Cookie name carrying the opaque refresh token (httpOnly). */
    private static final String REFRESH_COOKIE = "refresh_token";

    /** Refresh cookie lifetime: must match the token's 7-day server TTL. */
    private static final Duration REFRESH_COOKIE_TTL = Duration.ofDays(7);

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Builds the httpOnly refresh-token cookie.
     *
     * <p>{@code SameSite=None; Secure} is required because the SPA (Vercel)
     * and the API (Render) live on different sites: {@code Strict}/{@code Lax}
     * would silently drop the cookie on every cross-site XHR. Browsers treat
     * {@code localhost} as trustworthy, so {@code Secure} also works in dev.
     * Path is restricted to the auth endpoints so the token never travels
     * with regular API calls.</p>
     */
    private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
        return ResponseCookie.from(REFRESH_COOKIE, value == null ? "" : value)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(maxAge)
                .build();
    }

    /**
     * Resolves the refresh token: httpOnly cookie first (web client),
     * JSON body as fallback (legacy clients; scheduled for removal once the
     * SPA migration is complete).
     *
     * @throws BusinessException if neither source provides a usable token
     */
    private String resolveRefreshToken(String cookieToken, RefreshRequest body) {
        String token = (cookieToken != null && !cookieToken.isBlank())
                ? cookieToken
                : (body != null ? body.getRefreshToken() : null);
        // Tope de longitud: el token real son 43 chars base64url; esto solo
        // corta payloads abusivos antes de tocar la BD
        if (token == null || token.isBlank() || token.length() > 128) {
            throw new BusinessException("Refresh token requerido");
        }
        return token;
    }

    /**
     * Authenticates a user.
     *
     * @param request NIF + password
     * @return 200 with access + refresh tokens; 401 on bad credentials;
     *         400 if the account is unverified/suspended; 429 if rate-limited
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        // Solo se loguea el intento, nunca la contraseña
        logger.info("Intento de login para usuario: {}", request.getNif());
        AuthResponse response = authService.login(request,
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));

        // 2FA activado: aún no hay sesión; el frontend pedirá el código por email.
        if (response.isTwoFactorRequired()) {
            return ResponseEntity.ok(response);
        }

        logger.info("Login exitoso para usuario: {}", request.getNif());
        // El refresh token viaja TAMBIÉN como cookie httpOnly: el frontend web
        // no debe tocarlo desde JS. El campo del body queda como legado y se
        // eliminará cuando la SPA complete la migración.
        ResponseCookie cookie = buildRefreshCookie(response.getRefreshToken(), REFRESH_COOKIE_TTL);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Second step of a 2FA login: verifies the emailed code and opens the
     * session (sets the refresh cookie). Public, like {@code /login}.
     */
    @PostMapping("/login/2fa")
    public ResponseEntity<AuthResponse> verifyTwoFactor(
            @Valid @RequestBody com.ejemplo.vitsync.dto.TwoFactorLoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.verifyTwoFactor(request.getNif(), request.getCode(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        ResponseCookie cookie = buildRefreshCookie(response.getRefreshToken(), REFRESH_COOKIE_TTL);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
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
     * The token is read from the httpOnly cookie when present, falling back
     * to the JSON body for legacy clients.
     *
     * @param cookieToken refresh token from the httpOnly cookie (preferred)
     * @param request     legacy body fallback (optional)
     * @return 200 with new access token (+ rotated cookie); 400 if invalid/revoked
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.refresh(resolveRefreshToken(cookieToken, request),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"));
        // Rotación: la cookie se renueva con el nuevo token en cada refresh
        ResponseCookie cookie = buildRefreshCookie(response.getRefreshToken(), REFRESH_COOKIE_TTL);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    /**
     * Revokes the presented refresh token (single-device logout) and clears
     * the refresh cookie. Idempotent.
     *
     * @param cookieToken refresh token from the httpOnly cookie (preferred)
     * @param request     legacy body fallback (optional)
     * @return 200 always (no token-validity oracle)
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken,
            @RequestBody(required = false) RefreshRequest request) {
        // Logout idempotente: si no llega ningún token, solo limpiamos cookie
        String token = (cookieToken != null && !cookieToken.isBlank())
                ? cookieToken
                : (request != null ? request.getRefreshToken() : null);
        if (token != null && !token.isBlank() && token.length() <= 128) {
            authService.logout(token);
        }
        // Max-Age=0 borra la cookie en el navegador
        ResponseCookie cleared = buildRefreshCookie("", Duration.ZERO);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .body(Map.of("message", "Sesión cerrada"));
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
     * Lists the authenticated user's active sessions (device, IP, last use),
     * marking the current one (matched against the httpOnly refresh cookie).
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionResponse>> sessions(
            Authentication authentication,
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken) {
        return ResponseEntity.ok(authService.listSessions(authentication.getName(), cookieToken));
    }

    /** Revokes one of the authenticated user's sessions by id. */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Map<String, String>> revokeSession(
            Authentication authentication, @PathVariable Long id) {
        authService.revokeSession(authentication.getName(), id);
        return ResponseEntity.ok(Map.of("message", "Sesión cerrada"));
    }

    /** Revokes all sessions except the current one. */
    @PostMapping("/sessions/revoke-others")
    public ResponseEntity<Map<String, Object>> revokeOtherSessions(
            Authentication authentication,
            @CookieValue(value = REFRESH_COOKIE, required = false) String cookieToken) {
        int revoked = authService.revokeOtherSessions(authentication.getName(), cookieToken);
        return ResponseEntity.ok(Map.of(
                "message", "Otras sesiones cerradas",
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
