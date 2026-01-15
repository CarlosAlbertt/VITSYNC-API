package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    // Buscar todas las especialidades activas ordenadas por nombre
    List<Especialidad> findByActivoTrueOrderByNombreAsc();

    // Buscar todas las especialidades activas con médicos (JOIN FETCH para evitar
    // lazy loading)
    @Query("SELECT DISTINCT e FROM Especialidad e LEFT JOIN FETCH e.medicos WHERE e.activo = true ORDER BY e.nombre ASC")
    List<Especialidad> findAllActiveWithMedicos();

    // Buscar por slug
    Optional<Especialidad> findBySlug(String slug);

    // Buscar por slug con médicos (JOIN FETCH)
    @Query("SELECT e FROM Especialidad e LEFT JOIN FETCH e.medicos WHERE e.slug = :slug")
    Optional<Especialidad> findBySlugWithMedicos(@Param("slug") String slug);

    // Buscar por ID con médicos (JOIN FETCH)
    @Query("SELECT e FROM Especialidad e LEFT JOIN FETCH e.medicos WHERE e.id = :id")
    Optional<Especialidad> findByIdWithMedicos(@Param("id") Long id);

    // Buscar por código
    Optional<Especialidad> findByCodigo(String codigo);

    // Buscar por tipo
    List<Especialidad> findByTipoAndActivoTrue(String tipo);

    // Buscar especialidades que tengan médicos asignados
    @Query("SELECT DISTINCT e FROM Especialidad e LEFT JOIN FETCH e.medicos m WHERE e.activo = true AND m.role = 'PROFESIONAL'")
    List<Especialidad> findEspecialidadesConMedicos();
}
