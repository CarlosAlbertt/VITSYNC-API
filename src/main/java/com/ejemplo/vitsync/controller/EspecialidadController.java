package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.EspecialidadRequest;
import com.ejemplo.vitsync.dto.EspecialidadResponse;
import com.ejemplo.vitsync.service.EspecialidadService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    // GET /api/especialidades – Listar todas las especialidades activas
    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> getAllEspecialidades() {
        logger.info("Obteniendo todas las especialidades activas");
        List<EspecialidadResponse> especialidades = especialidadService.findAllActive()
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // GET /api/especialidades/admin – Listar TODAS las especialidades (incluidas inactivas)
    @GetMapping("/admin")
    public ResponseEntity<List<EspecialidadResponse>> getAllEspecialidadesAdmin() {
        logger.info("Obteniendo todas las especialidades (admin, incluye inactivas)");
        List<EspecialidadResponse> especialidades = especialidadService.findAll()
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // GET /api/especialidades/{id} – Obtener una especialidad por ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getEspecialidadById(@PathVariable Long id) {
        logger.info("Buscando especialidad con ID: {}", id);
        return especialidadService.findById(id)
                .map(e -> ResponseEntity.ok(EspecialidadResponse.fromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/especialidades/slug/{slug} – Obtener por slug
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getEspecialidadBySlug(@PathVariable String slug) {
        logger.info("Buscando especialidad con slug: {}", slug);
        return especialidadService.findBySlug(slug)
                .map(e -> ResponseEntity.ok(EspecialidadResponse.fromEntity(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/especialidades/tipo/{tipo} – Filtrar por tipo
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<EspecialidadResponse>> getEspecialidadesByTipo(@PathVariable String tipo) {
        logger.info("Buscando especialidades de tipo: {}", tipo);
        List<EspecialidadResponse> especialidades = especialidadService.findByTipo(tipo.toUpperCase())
                .stream()
                .map(EspecialidadResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(especialidades);
    }

    // ==================== ENDPOINTS DE ESCRITURA (ADMIN CRUD) ====================

    // POST /api/especialidades – Crear una nueva especialidad
    @PostMapping
    public ResponseEntity<?> createEspecialidad(@Valid @RequestBody EspecialidadRequest request) {
        logger.info("Creando nueva especialidad: {}", request.getNombre());
        try {
            EspecialidadResponse response = EspecialidadResponse.fromEntity(especialidadService.create(request));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al crear especialidad: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /api/especialidades/{id} – Actualizar una especialidad existente
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEspecialidad(@PathVariable Long id,
                                                 @Valid @RequestBody EspecialidadRequest request) {
        logger.info("Actualizando especialidad con ID: {}", id);
        try {
            EspecialidadResponse response = EspecialidadResponse.fromEntity(especialidadService.update(id, request));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al actualizar especialidad {}: {}", id, e.getMessage());
            String msg = e.getMessage();
            if (msg != null && msg.contains("no encontrada")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    // DELETE /api/especialidades/{id} – Eliminar una especialidad
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEspecialidad(@PathVariable Long id) {
        logger.info("Eliminando especialidad con ID: {}", id);
        try {
            especialidadService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            logger.warn("Error al eliminar especialidad {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // PATCH /api/especialidades/{id}/toggle-activo – Activar o desactivar una especialidad
    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        logger.info("Alternando estado activo de especialidad con ID: {}", id);
        try {
            EspecialidadResponse response = EspecialidadResponse.fromEntity(especialidadService.toggleActivo(id));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.warn("Error al alternar estado de especialidad {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
