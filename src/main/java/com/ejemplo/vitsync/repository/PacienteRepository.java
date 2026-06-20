package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    /** Resolves a patient by NIF (to associate the logged-in user to a new cita). */
    Optional<Paciente> findByNif(String nif);
}
