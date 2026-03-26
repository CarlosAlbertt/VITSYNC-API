package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.HistorialAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialAccesoRepository extends JpaRepository<HistorialAcceso, Long> {
}
