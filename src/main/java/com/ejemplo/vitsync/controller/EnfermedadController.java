package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.EnfermedadRequest;
import com.ejemplo.vitsync.dto.EnfermedadResponse;
import com.ejemplo.vitsync.service.EnfermedadService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/enfermedades")
public class EnfermedadController {

    private static final Logger logger = LoggerFactory.getLogger(EnfermedadController.class);

    private final EnfermedadService enfermedadService;

    public EnfermedadController(EnfermedadService enfermedadService) {
        this.enfermedadService = enfermedadService;
    }

    // ==================== ENDPOINTS PÚBLICOS (GET) ====================

    // GET /api/enfermedades – Listar todas las enfermedades activas
    @GetMapping
    public ResponseEntity<List<EnfermedadResponse>> getAllEnfermedades() {
        logger.info("Obteniendo todas las enfermedades activas");
        List<EnfermedadResponse> enfermedades = enfermedadService.findAllActive()
                .stream()
                .map(EnfermedadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enfermedades);
    }

    // GET /api/enfermedades/admin – Listar TODAS las enfermedades (incluidas inactivas)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EnfermedadResponse>> getAllEnfermedadesAdmin() {
        logger.info("Obteniendo todas las enfermedades (admin, incluye inactivas)");
        List<EnfermedadResponse> enfermedades = enfermedadService.findAll()
                .stream()
                .map(EnfermedadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(enfermedades);
    }

    // GET /api/enfermedades/{id} – Obtener una enfermedad por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEnfermedadById(@PathVariable Long id) {
        logger.info("Buscando enfermedad con ID: {}", id);
        return enfermedadService.findById(id)
                .map(e -> ResponseEntity.ok(EnfermedadResponse.fromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== ENDPOINTS DE ESCRITURA (ADMIN CRUD) ====================

    // POST /api/enfermedades – Crear una nueva enfermedad
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEnfermedad(@Valid @RequestBody EnfermedadRequest request) {
        logger.info("Creando nueva enfermedad: {}", request.getNombre());
        try {
            EnfermedadResponse response = EnfermedadResponse.fromEntity(enfermedadService.create(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al crear enfermedad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/enfermedades/{id} – Actualizar una enfermedad existente
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEnfermedad(@PathVariable Long id,
                                               @Valid @RequestBody EnfermedadRequest request) {
        logger.info("Actualizando enfermedad con ID: {}", id);
        try {
            EnfermedadResponse response = EnfermedadResponse.fromEntity(enfermedadService.update(id, request));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar enfermedad {}: {}", id, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("no encontrada")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // DELETE /api/enfermedades/{id} – Eliminar una enfermedad
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteEnfermedad(@PathVariable Long id) {
        logger.info("Eliminando enfermedad con ID: {}", id);
        try {
            enfermedadService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error al eliminar enfermedad {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /api/enfermedades/{id}/toggle-activo – Activar o desactivar una enfermedad
    @PatchMapping("/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        logger.info("Alternando estado activo de enfermedad con ID: {}", id);
        try {
            EnfermedadResponse response = EnfermedadResponse.fromEntity(enfermedadService.toggleActivo(id));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al alternar estado de enfermedad {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
