package com.ejemplo.vitsync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Entidad que representa una cita médica en el sistema.
 *
 * Relaciones JPA:
 * - Cada cita pertenece a un Paciente (@ManyToOne)
 * - Cada cita pertenece a un Médico (@ManyToOne)
 *
 * Tipos de cita: "Telemedicina" o "Presencial"
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "citas")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Paciente que solicita la cita */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Paciente paciente;

    /** Médico que atiende la cita */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Medico medico;

    /** Fecha y hora programada de la cita */
    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    /** Tipo de cita: "Telemedicina" o "Presencial" */
    private String tipo;

    /** Estado actual: "Programada", "Confirmada", "Completada", "Cancelada" */
    private String estado;

    /** Enlace para videoconsulta (solo para citas de tipo Telemedicina) */
    @Column(name = "enlace_videoconsulta")
    private String enlaceVideoconsulta;
}
