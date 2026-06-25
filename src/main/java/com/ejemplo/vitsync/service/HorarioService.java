package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.repository.CitaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cálculo de horarios disponibles para reservar cita.
 *
 * <p>Franja fija para todos los médicos: 08:00–17:00 en tramos de 30 minutos
 * (último hueco 16:30). De los huecos se restan las citas ya reservadas
 * (activas, no canceladas) del médico ese día, de modo que una hora ocupada
 * deja de ofrecerse y una cancelada vuelve a estar libre automáticamente.</p>
 */
@Service
public class HorarioService {

    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE = LocalTime.of(17, 0);
    private static final int TRAMO_MINUTOS = 30;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepository citaRepository;

    public HorarioService(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    /**
     * Horas libres ("HH:mm") de un médico para una fecha.
     *
     * @param medicoId id del médico, o {@code null} para "cualquier médico"
     *                 (en cuyo caso no se filtra por agenda concreta)
     * @param fecha    día solicitado
     */
    public List<String> getAvailableSlots(Long medicoId, LocalDate fecha) {
        List<String> todos = generarTramos();
        if (medicoId == null) {
            return todos; // "cualquier profesional": sin agenda concreta que filtrar
        }

        Set<String> ocupadas = citaRepository
                .findActiveByMedicoAndRange(medicoId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                .stream()
                .filter(c -> c.getFechaHora() != null)
                .map(c -> c.getFechaHora().toLocalTime().format(HHMM))
                .collect(Collectors.toSet());

        return todos.stream().filter(t -> !ocupadas.contains(t)).collect(Collectors.toList());
    }

    /** Genera los tramos 08:00, 08:30, … hasta el último antes del cierre (16:30). */
    private List<String> generarTramos() {
        List<String> tramos = new ArrayList<>();
        for (LocalTime t = APERTURA; t.isBefore(CIERRE); t = t.plusMinutes(TRAMO_MINUTOS)) {
            tramos.add(t.format(HHMM));
        }
        return tramos;
    }
}
