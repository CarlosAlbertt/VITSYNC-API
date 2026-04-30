package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.dto.IndicadorSaludDTO;
import com.ejemplo.vitsync.dto.IndicadorSaludRequest;
import com.ejemplo.vitsync.dto.SaludCategoriaDetalleResponse;
import com.ejemplo.vitsync.dto.SaludResumenResponse;
import com.ejemplo.vitsync.enums.CategoriaSalud;
import com.ejemplo.vitsync.model.IndicadorSalud;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.service.SaludService;
import com.ejemplo.vitsync.util.SecurityUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private final SaludService saludService;
    private final SecurityUtil securityUtil;

    public SaludController(SaludService saludService, SecurityUtil securityUtil) {
        this.saludService = saludService;
        this.securityUtil = securityUtil;
    }

    /**
     * Resumen de salud del paciente autenticado.
     * Devuelve el porcentaje y la variación (últimos 30 días) de cada categoría.
     *
     * GET /api/salud/resumen
     */
    @GetMapping("/resumen")
    public ResponseEntity<SaludResumenResponse> getResumen() {
        User currentUser = securityUtil.getCurrentUser();
        return ResponseEntity.ok(saludService.getResumenPaciente(currentUser.getId()));
    }

    /**
     * Detalle de una categoría: indicadores actuales + histórico para el gráfico.
     *
     * GET /api/salud/{categoria}
     * Valores válidos de {categoria}: CARDIO, METABOLISMO, ACTIVIDAD, NUTRICION, AUDICION, PULMONES
     */
    @GetMapping("/{categoria}")
    public ResponseEntity<SaludCategoriaDetalleResponse> getDetalle(
            @PathVariable CategoriaSalud categoria) {
        User currentUser = securityUtil.getCurrentUser();
        return ResponseEntity.ok(saludService.getDetalleCategoria(currentUser.getId(), categoria));
    }

    /**
     * Añade o actualiza un indicador de salud para el paciente autenticado.
     * Si ya existe un indicador con el mismo nombre y fecha, se actualiza (upsert).
     *
     * POST /api/salud/{categoria}/indicador
     */
    @PostMapping("/{categoria}/indicador")
    public ResponseEntity<IndicadorSaludDTO> addIndicador(
            @PathVariable CategoriaSalud categoria,
            @Valid @RequestBody IndicadorSaludRequest request) {
        User currentUser = securityUtil.getCurrentUser();
        IndicadorSalud saved = saludService.guardarIndicador(currentUser.getId(), categoria, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(IndicadorSaludDTO.fromEntity(saved));
    }
}
