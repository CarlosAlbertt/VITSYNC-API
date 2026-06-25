package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.repository.CitaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Cálculo de horarios disponibles para reservar cita.
 *
 * <p>Franja fija para todos los médicos: 08:00–17:00 en tramos de 30 minutos
 * (último hueco 16:30). De los huecos se restan: (1) las citas ya reservadas
 * (activas) del médico ese día, y (2) las horas ya pasadas (p. ej. hoy antes de
 * la hora actual). Así una hora ocupada o vencida deja de ofrecerse, y al
 * cancelar una cita su hueco vuelve a estar libre.</p>
 */
@Service
public class HorarioService {

    private static final LocalTime APERTURA = LocalTime.of(8, 0);
    private static final LocalTime CIERRE = LocalTime.of(17, 0);
    private static final int TRAMO_MINUTOS = 30;
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");
    // Zona horaria del negocio (España): "ahora" debe medirse en hora local, no UTC.
    private static final ZoneId ZONA = ZoneId.of("Europe/Madrid");

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
        LocalDateTime ahora = LocalDateTime.now(ZONA);

        Set<String> ocupadas = (medicoId == null)
                ? Collections.emptySet()
                : citaRepository
                        .findActiveByMedicoAndRange(medicoId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay())
                        .stream()
                        .filter(c -> c.getFechaHora() != null)
                        .map(c -> c.getFechaHora().toLocalTime().format(HHMM))
                        .collect(Collectors.toSet());

        return generarTramos().stream()
                // Excluir horas ya pasadas (hoy antes de la hora actual; días pasados → vacío)
                .filter(t -> LocalDateTime.of(fecha, t).isAfter(ahora))
                .map(t -> t.format(HHMM))
                .filter(s -> !ocupadas.contains(s))
                .collect(Collectors.toList());
    }

    /** Genera los tramos 08:00, 08:30, … hasta el último antes del cierre (16:30). */
    private List<LocalTime> generarTramos() {
        List<LocalTime> tramos = new ArrayList<>();
        for (LocalTime t = APERTURA; t.isBefore(CIERRE); t = t.plusMinutes(TRAMO_MINUTOS)) {
            tramos.add(t);
        }
        return tramos;
    }
}
