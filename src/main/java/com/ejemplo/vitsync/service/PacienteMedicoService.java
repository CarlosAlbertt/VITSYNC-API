package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.audit.Auditable;
import com.ejemplo.vitsync.enums.AuditAction;
import com.ejemplo.vitsync.exception.BusinessException;
import com.ejemplo.vitsync.exception.ResourceNotFoundException;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.PacienteMedico;
import com.ejemplo.vitsync.repository.MedicoRepository;
import com.ejemplo.vitsync.repository.PacienteMedicoRepository;
import com.ejemplo.vitsync.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PacienteMedicoService {

    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final PacienteMedicoRepository repository;

    public PacienteMedicoService(PacienteRepository pacienteRepository,
                                 MedicoRepository medicoRepository,
                                 PacienteMedicoRepository repository) {
        this.pacienteRepository = pacienteRepository;
        this.medicoRepository = medicoRepository;
        this.repository = repository;
    }

    public void asignarMedicoAPaciente(Long pacienteId, Long medicoId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id " + pacienteId));
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado con id " + medicoId));

        if (repository.existsByPacienteAndMedico(paciente, medico)) {
            throw new BusinessException("Ya existe la relación entre el paciente y el médico");
        }

        PacienteMedico relacion = new PacienteMedico();
        relacion.setPaciente(paciente);
        relacion.setMedico(medico);
        repository.save(relacion);
    }

    public List<Medico> getMedicosDePaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado con id " + pacienteId));
        return repository.findByPaciente(paciente).stream()
                .map(PacienteMedico::getMedico)
                .distinct()
                .collect(Collectors.toList());
    }

    @Auditable(action = AuditAction.VIEW_PATIENT_DATA, targetIdIndex = 0)
    public List<Paciente> getPacientesDeMedico(Long medicoId) {
        Medico medico = medicoRepository.findById(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico no encontrado con id " + medicoId));
        return repository.findByMedico(medico).stream()
                .map(PacienteMedico::getPaciente)
                .distinct()
                .collect(Collectors.toList());
    }
}

