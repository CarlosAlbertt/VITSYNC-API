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

    // ==================== ENDPOINTS ADMIN ====================

    // GET /api/especialidades/all - Listar TODAS las especialidades (incluidas
    // inactivas)
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EspecialidadResponse>> getAllEspecialidadesAdmin() {
        logger.info("Admin: Obteniendo todas las especialidades (incluidas inactivas)");
        List<EspecialidadResponse> especialidades = especialidadService.findAll()
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // POST /api/especialidades - Crear nueva especialidad
    @PostMapping
    public ResponseEntity<?> createEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        try {
            logger.info("Admin: Creando nueva especialidad con código: {}", request.getCodigo());
            EspecialidadResponse response = EspecialidadResponse.fromEntity(
                    especialidadService.create(request));
            logger.info("Especialidad creada exitosamente con ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error al crear especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/especialidades/{id} - Actualizar especialidad existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEspecialidad(@PathVariable Long id,
            @Valid @RequestBody EspecialidadRequest request) {
        try {
            logger.info("Admin: Actualizando especialidad con ID: {}", id);
            EspecialidadResponse response = EspecialidadResponse.fromEntity(
                    especialidadService.update(id, request));
            logger.info("Especialidad actualizada exitosamente con ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error al actualizar especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /api/especialidades/{id} - Eliminar especialidad
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEspecialidad(@PathVariable Long id) {
        try {
            logger.info("Admin: Eliminando especialidad con ID: {}", id);
            especialidadService.delete(id);
            logger.info("Especialidad eliminada exitosamente con ID: {}", id);
            return ResponseEntity.ok(Map.of("message", "Especialidad eliminada correctamente"));
        } catch (IllegalArgumentException e) {
            logger.error("Error al eliminar especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /api/especialidades/{id}/toggle-activo - Activar/desactivar
    // especialidad
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            logger.info("Admin: Toggling activo para especialidad con ID: {}", id);
            EspecialidadResponse response = EspecialidadResponse.fromEntity(
                    especialidadService.toggleActivo(id));
            logger.info("Especialidad ID: {} ahora está activo: {}", id, response.getActivo());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error al cambiar estado de especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
