package com.ejemplo.vitsync.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Join entity linking a {@link Paciente} with a {@link Medico}.
 *
 * <p>Both relations are {@code LAZY}: they were {@code EAGER}, which forced a
 * join across the JOINED inheritance subtables on every load and caused N+1
 * when listing relations (audit finding V19). Callers that need both ends use
 * the {@code @EntityGraph} fetch methods in the repository.</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "paciente_medico", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"paciente_id", "medico_id"})
})
public class PacienteMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Medico medico;

}
