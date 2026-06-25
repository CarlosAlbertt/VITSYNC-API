package com.ejemplo.vitsync.controller;

import com.ejemplo.vitsync.service.HorarioService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Horarios disponibles para reservar cita (público, lectura).
 *
 * <p>GET /api/horarios?medicoId=&fecha= → lista de horas "HH:mm" libres.
 * La disponibilidad real la calcula {@link HorarioService} (08:00–17:00 menos
 * las citas ya reservadas del médico ese día).</p>
 */
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    private final HorarioService horarioService;

    public HorarioController(HorarioService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping
    public List<String> getHorariosDisponibles(@RequestParam(required = false) String medicoId,
                                               @RequestParam(required = false) String fecha) {
        return horarioService.getAvailableSlots(parseMedicoId(medicoId), parseFecha(fecha));
    }

    /** Convierte el id de médico; {@code null} si es "any" o no es numérico. */
    private Long parseMedicoId(String medicoId) {
        if (medicoId == null || medicoId.isBlank() || "any".equalsIgnoreCase(medicoId)) {
            return null;
        }
        try {
            return Long.parseLong(medicoId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Toma la parte de fecha (YYYY-MM-DD) de una cadena ISO; hoy si no es válida. */
    private LocalDate parseFecha(String fecha) {
        if (fecha != null && fecha.length() >= 10) {
            try {
                return LocalDate.parse(fecha.substring(0, 10));
            } catch (java.time.format.DateTimeParseException ignored) {
                // cae al valor por defecto
            }
        }
        return LocalDate.now();
    }
}
