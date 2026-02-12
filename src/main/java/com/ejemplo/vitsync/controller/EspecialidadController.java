package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.EspecialidadResponse;
import com.ejemplo.vitsync.service.EspecialidadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/especialidades")

public class EspecialidadController {

    private static final Logger logger = LoggerFactory.getLogger(EspecialidadController.class);

    private final EspecialidadService especialidadService;

    public EspecialidadController(EspecialidadService especialidadService) {
        this.especialidadService = especialidadService;
    }

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

    // DEBUG: Endpoint temporal para diagnosticar problema con BD
    @GetMapping("/debug")
    public ResponseEntity<?> debugEspecialidades() {
        logger.info("=== DEBUG: Verificando conexión con BD ===");
        try {
            var todas = especialidadService.findAll();
            var activas = especialidadService.findAllActive();
            logger.info("Total en BD: {}, Activas: {}", todas.size(), activas.size());
            return ResponseEntity.ok(java.util.Map.of(
                    "totalEnBD", todas.size(),
                    "totalActivas", activas.size(),
                    "primeras5", todas.stream().limit(5).map(e -> java.util.Map.of("id", e.getId(), "nombre",
                            e.getNombre(), "activo", String.valueOf(e.getActivo()))).collect(Collectors.toList())));
        } catch (Exception ex) {
            logger.error("Error en debug: ", ex);
            return ResponseEntity.ok(java.util.Map.of("error", ex.getMessage()));
        }
    }
}
