package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.UserResponse;
import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.service.AdminUserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin-only user management ({@code /api/usuarios}, ADMIN role enforced in
 * {@code SecurityConfig}).
 *
 * <p>Listings are paginated ({@link Pageable}) to avoid loading the whole
 * users table (audit finding V19). Error handling is delegated to
 * {@code GlobalExceptionHandler}: {@code ResourceNotFoundException} → 404,
 * {@code DataIntegrityViolationException} → 409, validation → 400.</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@RestController
@RequestMapping("/api/usuarios")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // ==================== ENDPOINTS GET ====================

    /**
     * Lists users, paginated.
     *
     * @param pageable page request (e.g. {@code ?page=0&size=20&sort=id})
     * @return page of users as response DTOs
     */
    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        logger.info("Listando usuarios (página {})", pageable.getPageNumber());
        return ResponseEntity.ok(adminUserService.findAll(pageable).map(UserResponse::fromEntity));
    }

    /**
     * Returns a single user by id.
     *
     * @param id user id
     * @return the user, or 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return adminUserService.findById(id)
                .map(u -> ResponseEntity.ok(UserResponse.fromEntity(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lists users with a given role, paginated.
     *
     * @param rol      role name (ADMIN/MEDICO/PACIENTE)
     * @param pageable page request
     * @return page of users, or 400 if the role is invalid
     */
    @GetMapping("/rol/{rol}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String rol, Pageable pageable) {
        try {
            Role role = Role.valueOf(rol.toUpperCase());
            return ResponseEntity.ok(
                    adminUserService.findByRole(role, pageable).map(UserResponse::fromEntity));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Rol no válido. Valores permitidos: ADMIN, MEDICO, PACIENTE"));
        }
    }

    // ==================== ESCRITURA (ADMIN CRUD) ====================

    /**
     * Updates a user. Uniqueness and not-found are handled globally.
     *
     * @param id      user id
     * @param request validated update payload
     * @return the updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id,
                                                   @Valid @RequestBody UserUpdateRequest request) {
        logger.info("Actualizando usuario con ID: {}", id);
        return ResponseEntity.ok(UserResponse.fromEntity(adminUserService.update(id, request)));
    }

    /**
     * Deletes a user.
     *
     * @param id user id
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("Eliminando usuario con ID: {}", id);
        adminUserService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marks a user as verified/unverified without the email flow.
     *
     * @param id       user id
     * @param verified target verification state
     * @return the updated user
     */
    @PatchMapping("/{id}/verificar")
    public ResponseEntity<UserResponse> setVerified(@PathVariable Long id,
                                                    @RequestParam boolean verified) {
        logger.info("Marcando usuario {} como verificado: {}", id, verified);
        return ResponseEntity.ok(UserResponse.fromEntity(adminUserService.setVerified(id, verified)));
    }
}
