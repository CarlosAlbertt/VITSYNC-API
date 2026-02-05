package com.ejemplo.vitsync.service;

import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.PacienteMedico;
import com.ejemplo.vitsync.model.User;
import com.ejemplo.vitsync.repository.PacienteMedicoRepository;
import com.ejemplo.vitsync.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PacienteMedicoService {

    private final UserRepository userRepository;
    private final PacienteMedicoRepository repository;

    public PacienteMedicoService(UserRepository userRepository,
            PacienteMedicoRepository repository) {
        this.userRepository = userRepository;
        this.repository = repository;
    }

    public void asignarMedicoAPaciente(Long pacienteId, Long medicoId) throws Exception {
        Optional<User> paciente = userRepository.findById(pacienteId);
        Optional<User> medico = userRepository.findById(medicoId);

        if (paciente.get().getRole() != Role.PACIENTE)
            throw new Exception("El usuario con id " + pacienteId + " no es un paciente");
        if (medico.get().getRole() != Role.MEDICO)
            throw new Exception("El usuario con id " + medicoId + " no es un médico");

        // Verificar existencia (usando los repositorios específicos o cast si es
        // JOINED)
        if (!(paciente.get() instanceof Paciente)) {
            throw new Exception("El usuario encontrado no es una instancia de Paciente (Error de datos)");
        }
        if (!(medico.get() instanceof Medico)) {
            throw new Exception("El usuario encontrado no es una instancia de Medico (Error de datos)");
        }

        Paciente objPaciente = (Paciente) paciente.get();
        Medico objMedico = (Medico) medico.get();

        // Guardar relación
        if (repository.existsByPacienteAndMedico(objPaciente, objMedico)) {
            throw new Exception("Ya existe la relación");
        }

        PacienteMedico relacion = new PacienteMedico();
        relacion.setPaciente(objPaciente);
        relacion.setMedico(objMedico);
        repository.save(relacion);
    }

    public List<Medico> getMedicosDePaciente(Long pacienteId) {
        Paciente paciente = userRepository.findById(pacienteId)
                .map(user -> (Paciente) user)
                .orElseThrow(() -> new RuntimeException("Paciente no encontrado"));

        return repository.findByPaciente(paciente).stream()
                .map(PacienteMedico::getMedico)
                .collect(Collectors.toList());
    }

    public List<Paciente> getPacientesDeMedico(Long medicoId) {
        Medico medico = userRepository.findById(medicoId)
                .map(user -> (Medico) user)
                .orElseThrow(() -> new RuntimeException("Médico no encontrado"));

        return repository.findByMedico(medico).stream()
                .map(PacienteMedico::getPaciente)
                .collect(Collectors.toList());
    }
}
