package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPaciente_Nif(String nif);
}
