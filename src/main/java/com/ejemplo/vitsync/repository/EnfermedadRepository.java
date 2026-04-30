package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Enfermedad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EnfermedadRepository extends JpaRepository<Enfermedad, Long> {

    @Query("SELECT DISTINCT e FROM Enfermedad e LEFT JOIN FETCH e.tratamientos WHERE e.activo = true ORDER BY e.nombre ASC")
    List<Enfermedad> findAllActiveWithTratamientos();

    @Query("SELECT DISTINCT e FROM Enfermedad e LEFT JOIN FETCH e.tratamientos ORDER BY e.nombre ASC")
    List<Enfermedad> findAllWithTratamientos();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, Long id);
}
