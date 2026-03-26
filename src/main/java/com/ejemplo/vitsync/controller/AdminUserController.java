package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.UserResponse;
import com.ejemplo.vitsync.dto.UserUpdateRequest;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.service.AdminUserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/usuarios")
public class AdminUserController {

    private static final Logger logger = LoggerFactory.getLogger(AdminUserController.class);

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    // ==================== ENDPOINTS GET ====================

    // GET /api/usuarios – Listar todos los usuarios
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        logger.info("Obteniendo todos los usuarios");
        List<UserResponse> users = adminUserService.findAll()
                .stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    // GET /api/usuarios/{id} – Obtener usuario por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        logger.info("Buscando usuario con ID: {}", id);
        return adminUserService.findById(id)
                .map(u -> ResponseEntity.ok(UserResponse.fromEntity(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/usuarios/rol/{rol} – Filtrar por rol (ADMIN, MEDICO, PACIENTE)
    @GetMapping("/rol/{rol}")
    public ResponseEntity<?> getUsersByRole(@PathVariable String rol) {
        logger.info("Filtrando usuarios por rol: {}", rol);
        try {
            Role role = Role.valueOf(rol.toUpperCase());
            List<UserResponse> users = adminUserService.findByRole(role)
                    .stream()
                    .map(UserResponse::fromEntity)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(users);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Rol no válido: " + rol + ". Valores permitidos: ADMIN, MEDICO, PACIENTE"));
        }
    }

    // ==================== ENDPOINTS DE ESCRITURA (ADMIN CRUD) ====================

    // PUT /api/usuarios/{id} – Actualizar datos de usuario
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                         @Valid @RequestBody UserUpdateRequest request) {
        logger.info("Actualizando usuario con ID: {}", id);
        try {
            UserResponse response = UserResponse.fromEntity(adminUserService.update(id, request));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar usuario {}: {}", id, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("no encontrado")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // DELETE /api/usuarios/{id} – Eliminar usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        logger.info("Eliminando usuario con ID: {}", id);
        try {
            adminUserService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error al eliminar usuario {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /api/usuarios/{id}/verificar – Marcar usuario como verificado/no verificado
    @PatchMapping("/{id}/verificar")
    public ResponseEntity<?> setVerified(@PathVariable Long id,
                                          @RequestParam boolean verified) {
        logger.info("Marcando usuario {} como verificado: {}", id, verified);
        try {
            UserResponse response = UserResponse.fromEntity(adminUserService.setVerified(id, verified));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al verificar usuario {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
