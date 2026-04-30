package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    java.util.List<Cita> findByPacienteId(Long pacienteId);

    java.util.List<Cita> findByMedicoId(Long medicoId);
}
