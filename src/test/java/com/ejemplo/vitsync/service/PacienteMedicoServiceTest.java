package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.PacienteMedico;
import com.ejemplo.vitsync.repository.MedicoRepository;
import com.ejemplo.vitsync.repository.PacienteMedicoRepository;
import com.ejemplo.vitsync.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PacienteMedicoService}: clinician-patient
 * assignment and bidirectional lookups.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PacienteMedicoService")
class PacienteMedicoServiceTest {

    @Mock PacienteRepository pacienteRepository;
    @Mock MedicoRepository medicoRepository;
    @Mock PacienteMedicoRepository repository;

    @InjectMocks PacienteMedicoService service;

    private Paciente paciente;
    private Medico medico;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(1L);
        medico = new Medico();
        medico.setId(2L);
    }

    // ---- asignarMedicoAPaciente ----

    @Test
    @DisplayName("asignar: crea la relación cuando ambos existen y no estaba")
    void asignar_ok_savesRelation() throws Exception {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(2L)).thenReturn(Optional.of(medico));
        when(repository.existsByPacienteAndMedico(paciente, medico)).thenReturn(false);

        service.asignarMedicoAPaciente(1L, 2L);

        ArgumentCaptor<PacienteMedico> captor = ArgumentCaptor.forClass(PacienteMedico.class);
        verify(repository).save(captor.capture());
        assertSame(paciente, captor.getValue().getPaciente());
        assertSame(medico, captor.getValue().getMedico());
    }

    @Test
    @DisplayName("asignar: paciente inexistente → excepción, no toca repo de relaciones")
    void asignar_pacienteMissing_throws() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.asignarMedicoAPaciente(1L, 2L));
        assertTrue(ex.getMessage().contains("Paciente no encontrado"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("asignar: médico inexistente → excepción")
    void asignar_medicoMissing_throws() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(2L)).thenReturn(Optional.empty());

        Exception ex = assertThrows(Exception.class, () -> service.asignarMedicoAPaciente(1L, 2L));
        assertTrue(ex.getMessage().contains("Médico no encontrado"));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("asignar: relación duplicada → excepción sin save")
    void asignar_duplicateRelation_throws() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(medicoRepository.findById(2L)).thenReturn(Optional.of(medico));
        when(repository.existsByPacienteAndMedico(paciente, medico)).thenReturn(true);

        Exception ex = assertThrows(Exception.class, () -> service.asignarMedicoAPaciente(1L, 2L));
        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(repository, never()).save(any());
    }

    // ---- getMedicosDePaciente ----

    @Test
    @DisplayName("getMedicosDePaciente: devuelve médicos sin duplicados")
    void getMedicosDePaciente_returnsDistinct() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        PacienteMedico rel1 = new PacienteMedico();
        rel1.setMedico(medico);
        PacienteMedico rel2 = new PacienteMedico();
        rel2.setMedico(medico); // misma instancia: distinct() debe colapsarla
        when(repository.findByPaciente(paciente)).thenReturn(List.of(rel1, rel2));

        List<Medico> result = service.getMedicosDePaciente(1L);

        assertEquals(1, result.size());
        assertSame(medico, result.get(0));
    }

    @Test
    @DisplayName("getMedicosDePaciente: paciente inexistente → RuntimeException")
    void getMedicosDePaciente_missing_throws() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getMedicosDePaciente(1L));
    }

    // ---- getPacientesDeMedico ----

    @Test
    @DisplayName("getPacientesDeMedico: devuelve pacientes del médico")
    void getPacientesDeMedico_returnsPatients() {
        when(medicoRepository.findById(2L)).thenReturn(Optional.of(medico));
        PacienteMedico rel = new PacienteMedico();
        rel.setPaciente(paciente);
        when(repository.findByMedico(medico)).thenReturn(List.of(rel));

        List<Paciente> result = service.getPacientesDeMedico(2L);

        assertEquals(1, result.size());
        assertSame(paciente, result.get(0));
    }

    @Test
    @DisplayName("getPacientesDeMedico: médico inexistente → RuntimeException")
    void getPacientesDeMedico_missing_throws() {
        when(medicoRepository.findById(2L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getPacientesDeMedico(2L));
    }
}
