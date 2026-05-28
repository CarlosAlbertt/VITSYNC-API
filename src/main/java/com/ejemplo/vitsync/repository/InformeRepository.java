package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Informe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InformeRepository extends JpaRepository<Informe, Long> {
    List<Informe> findByPaciente_Nif(String nif);
}
