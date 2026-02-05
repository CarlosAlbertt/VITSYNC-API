package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.PacienteMedico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PacienteMedicoRepository extends JpaRepository<PacienteMedico, Long> {

    List<PacienteMedico> findByPaciente(Paciente paciente);
    Optional<PacienteMedico> findById(Long id);
    List<PacienteMedico> findByMedico(Medico medico);
    
    boolean existsByPacienteAndMedico(Paciente paciente, Medico medico);
}
