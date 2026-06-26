package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPaciente_Nif(String nif);

    /** Citas ACTIVAS (no canceladas) de un médico en un rango (un día) — para calcular huecos libres. */
    @Query("SELECT c FROM Cita c WHERE c.medico.id = :medicoId "
            + "AND c.fechaHora >= :start AND c.fechaHora < :end "
            + "AND c.estado <> 'Cancelada'")
    List<Cita> findActiveByMedicoAndRange(@Param("medicoId") Long medicoId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    /** ¿Ya hay una cita activa para ese médico a esa hora exacta? (chequeo previo de concurrencia). */
    @Query("SELECT COUNT(c) > 0 FROM Cita c WHERE c.medico.id = :medicoId "
            + "AND c.fechaHora = :fechaHora AND c.estado <> 'Cancelada'")
    boolean existsActiveSlot(@Param("medicoId") Long medicoId,
                             @Param("fechaHora") LocalDateTime fechaHora);
}
