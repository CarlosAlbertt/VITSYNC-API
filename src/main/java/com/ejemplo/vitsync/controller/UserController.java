package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.config.SecurityUtils;
import com.ejemplo.vitsync.dto.ProfileUpdateRequest;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.service.IUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Self-service user endpoints (profile, avatar).
 *
 * <p>All routes live under {@code /VitSync-app} and require authentication.
 * Ownership is enforced with {@link SecurityUtils#requireSelfOrAdmin} so a
 * user can only read/modify their own record (IDOR prevention, audit finding
 * V03). The previous {@code POST} endpoint that accepted a raw {@code User}
 * (privilege-escalation vector V17) and the broken {@code DELETE} have been
 * removed; user CRUD for admins lives in {@code AdminUserController}.</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/VitSync-app")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final IUserService userService;
    private final SecurityUtils securityUtils;
    private final com.ejemplo.vitsync.service.AuthService authService;
    private final com.ejemplo.vitsync.service.AccountRecoveryService accountRecoveryService;

    public UserController(IUserService userService, SecurityUtils securityUtils,
                          com.ejemplo.vitsync.service.AuthService authService,
                          com.ejemplo.vitsync.service.AccountRecoveryService accountRecoveryService) {
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.authService = authService;
        this.accountRecoveryService = accountRecoveryService;
    }

    /**
     * Lists all users. ADMIN only — exposing the full user base to any
     * authenticated user violated data minimisation (GDPR Art. 5.1.c).
     *
     * @return all users
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> findAll() {
        return userService.findAll();
    }

    /**
     * Returns a single user. Allowed only to the owner or an admin.
     *
     * @param id user id from the path
     * @return the user, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
        securityUtils.requireSelfOrAdmin(id);
        User user = userService.findById(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    /**
     * Updates the caller's own profile (partial update of safe fields).
     *
     * @param id      target user id (must be the caller's own, or admin)
     * @param request validated partial profile
     * @return the updated user, or 404 if not found
     */
    @PutMapping("/api/users/{id}/profile")
    public ResponseEntity<User> updateUserProfile(@PathVariable Long id,
                                                  @Valid @RequestBody ProfileUpdateRequest request) {
        securityUtils.requireSelfOrAdmin(id);

        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        // Solo se aplican los campos presentes (actualización parcial)
        if (isPresent(request.getName())) user.setName(request.getName().trim());
        if (isPresent(request.getFirstName())) user.setFirstName(request.getFirstName().trim());
        if (isPresent(request.getSecondName())) user.setSecondName(request.getSecondName().trim());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (isPresent(request.getPhone())) user.setPhone(request.getPhone().trim());
        if (isPresent(request.getAddress())) user.setAddress(request.getAddress().trim());
        if (isPresent(request.getPostCode())) user.setPostCode(request.getPostCode().trim());
        if (isPresent(request.getCountry())) user.setCountry(request.getCountry().trim());

        // Datos médicos: solo si el usuario es Paciente. Se aplican cuando el
        // campo viene en la petición (null = no tocar); cadena vacía = limpiar.
        // Se cifran en reposo automáticamente (SensitiveDataConverter).
        if (user instanceof Paciente paciente) {
            if (request.getGrupoSanguineo() != null) paciente.setGrupoSanguineo(emptyToNull(request.getGrupoSanguineo()));
            if (request.getAlergias() != null) paciente.setAlergias(emptyToNull(request.getAlergias()));
            if (request.getCondicionesPrevias() != null) paciente.setCondicionesPrevias(emptyToNull(request.getCondicionesPrevias()));
            if (request.getContactoEmergencia() != null) paciente.setContactoEmergencia(emptyToNull(request.getContactoEmergencia()));
        }

        userService.saveUser(user);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates the caller's avatar URL.
     *
     * @param id      target user id (must be the caller's own, or admin)
     * @param payload {@code {"avatarUrl": "..."}}
     * @return 200 on success, 404 if the user does not exist
     */
    @PatchMapping("/api/users/{id}/avatar")
    public ResponseEntity<Void> updateAvatar(@PathVariable Long id,
                                             @RequestBody @Valid AvatarPayload payload) {
        securityUtils.requireSelfOrAdmin(id);
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        userService.updateAvatar(id, payload.avatarUrl());
        return ResponseEntity.ok().build();
    }

    /** Avatar update body with a bounded URL. */
    public record AvatarPayload(
            @NotBlank(message = "avatarUrl es obligatorio")
            @Size(max = 512, message = "URL demasiado larga")
            String avatarUrl) {
    }

    /**
     * Changes the caller's own password. Verifies the current password and
     * enforces the password policy (delegated to AuthService).
     */
    @PatchMapping("/api/users/{id}/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody com.ejemplo.vitsync.dto.ChangePasswordRequest request) {
        securityUtils.requireSelfOrAdmin(id);
        authService.changePassword(id, request);
        return ResponseEntity.ok(Map.of("message", "Contraseña actualizada"));
    }

    /**
     * Activa o desactiva la verificación en dos pasos (2FA por email) del
     * propio usuario. Cuerpo: {@code {"enabled": true|false}}.
     */
    @PutMapping("/api/users/{id}/security/2fa")
    public ResponseEntity<Map<String, Object>> setTwoFactor(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        securityUtils.requireSelfOrAdmin(id);
        User user = userService.findById(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        user.setTwoFactorEnabled(enabled);
        userService.saveUser(user);
        return ResponseEntity.ok(Map.of(
                "twoFactorEnabled", enabled,
                "message", enabled ? "Verificación en dos pasos activada"
                                   : "Verificación en dos pasos desactivada"));
    }

    /**
     * Stores (or replaces) the caller's two security questions used for
     * password recovery. The answers are hashed with BCrypt and never returned.
     * Body: {@code {"q1","a1","q2","a2"}}.
     */
    @PostMapping("/api/users/{id}/security/questions")
    public ResponseEntity<Map<String, String>> saveSecurityQuestions(
            @PathVariable Long id,
            @Valid @RequestBody com.ejemplo.vitsync.dto.SecurityQuestionsRequest request) {
        securityUtils.requireSelfOrAdmin(id);
        accountRecoveryService.saveQuestions(id, request);
        return ResponseEntity.ok(Map.of("message", "Preguntas de seguridad guardadas"));
    }

    // ─── Endpoints aún sin implementar (devuelven 501 explícito) ──────

    /**
     * Toggle 2FA — not implemented yet. Returns 501 so the frontend gets a
     * clear signal instead of a fake 200 (audit finding V21).
     */
    @PutMapping("/api/users/security/2fa")
    public ResponseEntity<Map<String, String>> toggle2FA() {
        return ResponseEntity.status(501).body(Map.of("message", "2FA no implementado todavía"));
    }

    /** Account suspension — not implemented yet (returns 501). */
    @PutMapping("/api/users/status")
    public ResponseEntity<Map<String, String>> suspendUserAccount() {
        return ResponseEntity.status(501).body(Map.of("message", "Suspensión no implementada todavía"));
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    /** Trims and converts blank strings to null (para poder limpiar campos opcionales). */
    private String emptyToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
