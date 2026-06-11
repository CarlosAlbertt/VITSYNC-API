package com.ejemplo.vitsync.repository;

import com.ejemplo.vitsync.model.Medico;
import com.ejemplo.vitsync.model.Paciente;
import com.ejemplo.vitsync.model.PacienteMedico;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for the patient–clinician relationship.
 *
 * <p>The {@code @EntityGraph} on the fetch methods eagerly loads only the side
 * the caller needs in a single query, avoiding the N+1 that the previous
 * {@code EAGER} mappings caused (audit finding V19).</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
public interface PacienteMedicoRepository extends JpaRepository<PacienteMedico, Long> {

    /**
     * Relations of a patient, fetching the {@code medico} side in one query.
     *
     * @param paciente patient
     * @return relations with the clinician initialised
     */
    @EntityGraph(attributePaths = "medico")
    List<PacienteMedico> findByPaciente(Paciente paciente);

    Optional<PacienteMedico> findById(Long id);

    /**
     * Relations of a clinician, fetching the {@code paciente} side in one query.
     *
     * @param medico clinician
     * @return relations with the patient initialised
     */
    @EntityGraph(attributePaths = "paciente")
    List<PacienteMedico> findByMedico(Medico medico);

    boolean existsByPacienteAndMedico(Paciente paciente, Medico medico);
}
