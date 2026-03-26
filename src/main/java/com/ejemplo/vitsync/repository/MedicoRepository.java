package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Medico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    // Buscar todos los médicos activos con su especialidad cargada (evita lazy loading)
    @Query("SELECT m FROM Medico m LEFT JOIN FETCH m.especialidad WHERE m.activo = true ORDER BY m.name ASC")
    List<Medico> findAllActiveWithEspecialidad();

    // Buscar TODOS los médicos (activos e inactivos) con su especialidad (admin)
    @Query("SELECT m FROM Medico m LEFT JOIN FETCH m.especialidad ORDER BY m.name ASC")
    List<Medico> findAllWithEspecialidad();

    // Buscar médico por ID con especialidad
    @Query("SELECT m FROM Medico m LEFT JOIN FETCH m.especialidad WHERE m.id = :id")
    Optional<Medico> findByIdWithEspecialidad(Long id);

    // Buscar médicos por especialidad
    @Query("SELECT m FROM Medico m LEFT JOIN FETCH m.especialidad WHERE m.especialidad.id = :especialidadId AND m.activo = true")
    List<Medico> findByEspecialidadId(Long especialidadId);

    // Unicidad de número de colegiado
    boolean existsByNumeroColegiado(String numeroColegiado);

    boolean existsByNumeroColegiadoAndIdNot(String numeroColegiado, Long id);

    Optional<Medico> findByNumeroColegiado(String numeroColegiado);
}
