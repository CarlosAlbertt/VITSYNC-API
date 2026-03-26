package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.service.IUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/VitSync-app")

public class UserController {

    private final IUserService userService;

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @PostMapping
    public void saveUser(@RequestBody User user) {
        userService.saveUser(user);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable User user) {
        userService.deleteUser(user);
    }

    // --- Endpoints para Perfil de Usuario ---

    @PutMapping("/api/users/profile")
    public org.springframework.http.ResponseEntity<String> updateUserProfile() {
        // TODO: Implement actual update logic integrating with user info
        return org.springframework.http.ResponseEntity.ok("Profile updated successfully");
    }

    @PutMapping("/api/users/security/2fa")
    public org.springframework.http.ResponseEntity<String> toggle2FA() {
        // TODO: Implement 2FA toggle
        return org.springframework.http.ResponseEntity.ok("2FA status updated");
    }

    @PutMapping("/api/users/status")
    public org.springframework.http.ResponseEntity<String> suspendUserAccount() {
        // TODO: Implement account suspension logic
        return org.springframework.http.ResponseEntity.ok("User account suspended");
    }

    @GetMapping("/api/users/access-history")
    public org.springframework.http.ResponseEntity<List<Object>> getUserAccessHistory() {
        // TODO: Implement fetching history from HistorialAccesoService once ready
        return org.springframework.http.ResponseEntity.ok(List.of());
    }

    @PatchMapping("/api/users/{id}/avatar")
    public org.springframework.http.ResponseEntity<?> updateAvatar(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        try {
            User user = userService.findById(id);
            if(user != null) {
                user.setAvatarUrl(payload.get("avatarUrl"));
                userService.saveUser(user);
                return org.springframework.http.ResponseEntity.ok().build();
            }
            return org.springframework.http.ResponseEntity.notFound().build();
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().build();
        }
    }
}