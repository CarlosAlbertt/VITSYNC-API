package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Enfermedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnfermedadRepository extends JpaRepository<Enfermedad, Long> {

    /** Enfermedades activas (catálogo público). */
    List<Enfermedad> findByActivoTrue();
}
