package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.EspecialidadRequest;
import com.ejemplo.vitsync.dto.EspecialidadResponse;
import com.ejemplo.vitsync.service.EspecialidadService;
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
@RequestMapping("/api/especialidades")
public class EspecialidadController {

    private static final Logger logger = LoggerFactory.getLogger(EspecialidadController.class);

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

    // ==================== ENDPOINTS PÚBLICOS (GET) ====================

    // GET /api/especialidades - Listar todas las especialidades activas
    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> getAllEspecialidades() {
        logger.info("Obteniendo todas las especialidades activas");
        List<EspecialidadResponse> especialidades = especialidadService.findAllActive()
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // GET /api/especialidades/{id} - Obtener una especialidad por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEspecialidadById(@PathVariable Long id) {
        logger.info("Buscando especialidad con ID: {}", id);
        return especialidadService.findById(id)
                .map(e -> ResponseEntity.ok(EspecialidadResponse.fromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/especialidades/slug/{slug} - Obtener por slug
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getEspecialidadBySlug(@PathVariable String slug) {
        logger.info("Buscando especialidad con slug: {}", slug);
        return especialidadService.findBySlug(slug)
                .map(e -> ResponseEntity.ok(EspecialidadResponse.fromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/especialidades/tipo/{tipo} - Filtrar por tipo
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<EspecialidadResponse>> getEspecialidadesByTipo(@PathVariable String tipo) {
        logger.info("Buscando especialidades de tipo: {}", tipo);
        List<EspecialidadResponse> especialidades = especialidadService.findByTipo(tipo.toUpperCase())
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // ==================== ENDPOINTS ADMIN (requieren role ADMIN)
    // ====================

    // GET /api/especialidades/admin - Listar todas (incluidas inactivas)
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EspecialidadResponse>> getAllEspecialidadesAdmin() {
        logger.info("ADMIN: Obteniendo todas las especialidades (incluidas inactivas)");
        List<EspecialidadResponse> especialidades = especialidadService.findAll()
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // POST /api/especialidades - Crear nueva especialidad
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        logger.info("ADMIN: Creando nueva especialidad: {}", request.getNombre());
        try {
            var created = especialidadService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(EspecialidadResponse.fromEntity(created));
        } catch (IllegalArgumentException e) {
            logger.warn("Error al crear especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/especialidades/{id} - Actualizar especialidad
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateEspecialidad(@PathVariable Long id,
            @Valid @RequestBody EspecialidadRequest request) {
        logger.info("ADMIN: Actualizando especialidad ID: {}", id);
        try {
            var updated = especialidadService.update(id, request);
            return ResponseEntity.ok(EspecialidadResponse.fromEntity(updated));
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/especialidades/{id} - Eliminar especialidad
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEspecialidad(@PathVariable Long id) {
        logger.info("ADMIN: Eliminando especialidad ID: {}", id);
        try {
            especialidadService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error al eliminar especialidad: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /api/especialidades/{id}/toggle-activo - Activar/desactivar
    @PatchMapping("/{id}/toggle-activo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        logger.info("ADMIN: Toggling activo para especialidad ID: {}", id);
        try {
            var toggled = especialidadService.toggleActivo(id);
            return ResponseEntity.ok(EspecialidadResponse.fromEntity(toggled));
        } catch (IllegalArgumentException e) {
            logger.warn("Error al toggle activo: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
