package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Informe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InformeRepository extends JpaRepository<Informe, Long> {

    java.util.List<Informe> findByPacienteId(Long pacienteId);

    java.util.List<Informe> findByMedicoId(Long medicoId);
}
