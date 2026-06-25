package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Cita;
import com.ejemplo.vitsync.repository.CitaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link HorarioService}: generación de huecos 08:00–17:00 (30 min)
 * y exclusión de los ya reservados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HorarioService — huecos disponibles")
class HorarioServiceTest {

    @Mock private CitaRepository citaRepository;
    @InjectMocks private HorarioService horarioService;

    private final LocalDate dia = LocalDate.of(2026, 7, 1);

    @Test
    @DisplayName("sin médico (any): devuelve todos los tramos 08:00..16:30")
    void anyMedico_devuelveTodos() {
        List<String> slots = horarioService.getAvailableSlots(null, dia);

        assertEquals(18, slots.size());           // 08:00..16:30 cada 30 min
        assertEquals("08:00", slots.get(0));
        assertEquals("16:30", slots.get(slots.size() - 1));
        assertFalse(slots.contains("17:00"));     // el cierre no es un hueco reservable
        verifyNoInteractions(citaRepository);
    }

    @Test
    @DisplayName("con médico: excluye las horas ya reservadas (activas)")
    void conMedico_excluyeOcupadas() {
        Cita c1 = new Cita();
        c1.setFechaHora(LocalDateTime.of(dia, java.time.LocalTime.of(9, 0)));
        Cita c2 = new Cita();
        c2.setFechaHora(LocalDateTime.of(dia, java.time.LocalTime.of(12, 30)));
        when(citaRepository.findActiveByMedicoAndRange(eq(5L), any(), any()))
                .thenReturn(List.of(c1, c2));

        List<String> slots = horarioService.getAvailableSlots(5L, dia);

        assertFalse(slots.contains("09:00"));
        assertFalse(slots.contains("12:30"));
        assertTrue(slots.contains("08:00"));
        assertTrue(slots.contains("16:30"));
        assertEquals(16, slots.size());           // 18 - 2 ocupadas
    }
}
