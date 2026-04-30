package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.enums.CategoriaSalud;
import com.ejemplo.vitsync.model.IndicadorSalud;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface IndicadorSaludRepository extends JpaRepository<IndicadorSalud, Long> {

    List<IndicadorSalud> findByPacienteId(Long pacienteId);

    List<IndicadorSalud> findByPacienteIdAndCategoria(Long pacienteId, CategoriaSalud categoria);

    Optional<IndicadorSalud> findByPacienteIdAndCategoriaAndNombreAndFecha(
            Long pacienteId, CategoriaSalud categoria, String nombre, LocalDate fecha);
}
