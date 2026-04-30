package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.UserResponse;
import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.service.IUserService;
import com.ejemplo.vitsync.util.SecurityUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/VitSync-app")
public class UserController {

    private final IUserService userService;
    private final SecurityUtil securityUtil;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(IUserService userService, SecurityUtil securityUtil) {
        this.userService = userService;
        this.securityUtil = securityUtil;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> findAll() {
        return userService.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable Long id) {
        securityUtil.assertOwnerOrAdmin(id);
        User user = userService.findById(id);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    /** Solo ADMIN puede crear usuarios directamente (el registro público es vía /api/auth/register). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> saveUser(@RequestBody User user) {
        userService.saveUser(user);
        return ResponseEntity.status(201).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        securityUtil.assertOwnerOrAdmin(id);
        User user = userService.findById(id);
        if (user == null) return ResponseEntity.notFound().build();
        userService.deleteUser(user);
        return ResponseEntity.noContent().build();
    }

    // --- Endpoints para Perfil de Usuario ---

    @PutMapping("/api/users/{id}/profile")
    public ResponseEntity<UserResponse> updateUserProfile(@PathVariable Long id,
                                                          @Valid @RequestBody UserUpdateRequest request) {
        securityUtil.assertOwnerOrAdmin(id);
        User updated = userService.updateProfile(id, request);
        return ResponseEntity.ok(UserResponse.fromEntity(updated));
    }

    @PutMapping("/api/users/{id}/security/2fa")
    public ResponseEntity<Void> toggle2FA(@PathVariable Long id, @RequestBody Map<String, Boolean> payload) {
        securityUtil.assertOwnerOrAdmin(id);
        userService.setTwoFactorEnabled(id, payload.get("enabled"));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/users/{id}/security/questions")
    public ResponseEntity<Void> saveSecurityQuestions(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        securityUtil.assertOwnerOrAdmin(id);
        userService.saveSecurityQuestions(id, payload);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/users/status")
    public ResponseEntity<String> suspendUserAccount() {
        User current = securityUtil.getCurrentUser();
        userService.suspendUser(current.getId());
        return ResponseEntity.ok("User account suspended");
    }

    @GetMapping("/api/users/me/export")
    public ResponseEntity<Map<String, Object>> exportMyData() {
        User current = securityUtil.getCurrentUser();
        return ResponseEntity.ok(userService.exportUserData(current.getId()));
    }

    @GetMapping("/api/users/access-history")
    public ResponseEntity<List<Object>> getUserAccessHistory() {
        // TODO: Implement fetching history from HistorialAccesoService once ready
        return ResponseEntity.ok(List.of());
    }

    @PatchMapping("/api/users/{id}/avatar")
    public ResponseEntity<UserResponse> updateAvatar(@PathVariable Long id,
                                                     @RequestBody Map<String, String> payload) {
        securityUtil.assertOwnerOrAdmin(id);
        User user = userService.findById(id);
        if (user == null) return ResponseEntity.notFound().build();
        user.setAvatarUrl(payload.get("avatarUrl"));
        userService.saveUser(user);
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }

    @PatchMapping("/api/users/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long id,
                                               @RequestBody Map<String, String> payload) {
        securityUtil.assertOwnerOrAdmin(id);
        String currentPassword = payload.get("currentPassword");
        String newPassword = payload.get("newPassword");
        userService.changePassword(id, currentPassword, newPassword);
        return ResponseEntity.noContent().build();
    }
}
