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

    @PutMapping("/api/users/{id}/profile")
    public org.springframework.http.ResponseEntity<?> updateUserProfile(@PathVariable Long id, @RequestBody java.util.Map<String, Object> payload) {
        try {
            User user = userService.findById(id);
            if (user != null) {
                if (payload.containsKey("name") && payload.get("name") != null && !((String)payload.get("name")).isBlank()) user.setName((String) payload.get("name"));
                if (payload.containsKey("firstName") && payload.get("firstName") != null && !((String)payload.get("firstName")).isBlank()) user.setFirstName((String) payload.get("firstName"));
                if (payload.containsKey("secondName") && payload.get("secondName") != null && !((String)payload.get("secondName")).isBlank()) user.setSecondName((String) payload.get("secondName"));
                
                if (payload.containsKey("gender") && payload.get("gender") != null) {
                    try {
                        user.setGender(com.ejemplo.vitsync.enums.Gender.valueOf(payload.get("gender").toString()));
                    } catch (Exception ignored) {}
                }
                
                if (payload.containsKey("phone") && payload.get("phone") != null && !((String)payload.get("phone")).isBlank()) user.setPhone((String) payload.get("phone"));
                if (payload.containsKey("address") && payload.get("address") != null && !((String)payload.get("address")).isBlank()) user.setAddress((String) payload.get("address"));
                if (payload.containsKey("postCode") && payload.get("postCode") != null && !((String)payload.get("postCode")).isBlank()) user.setPostCode((String) payload.get("postCode"));
                if (payload.containsKey("country") && payload.get("country") != null && !((String)payload.get("country")).isBlank()) user.setCountry((String) payload.get("country"));
                
                userService.saveUser(user);
                return org.springframework.http.ResponseEntity.ok(user);
            }
            return org.springframework.http.ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error actualizando perfil para usuario {}: {}", id, e.getMessage(), e);
            return org.springframework.http.ResponseEntity.internalServerError().body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Error interno"));
        }
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
                userService.updateAvatar(id, payload.get("avatarUrl"));
                return org.springframework.http.ResponseEntity.ok().build();
            }
            return org.springframework.http.ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Error actualizando avatar para usuario {}: {}", id, e.getMessage(), e);
            return org.springframework.http.ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Error desconocido"));
        }
    }
}