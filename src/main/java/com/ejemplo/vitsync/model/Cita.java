package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "citas")
public class Cita {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "medico_id")
    private Long medicoId;

    @Column(name = "fecha_hora")
    private LocalDateTime fechaHora;

    private String tipo; // Telemedicina / Presencial

    private String estado;

    @Column(name = "enlace_videoconsulta")
    private String enlaceVideoconsulta;
}
