package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "informes")
public class Informe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id")
    private Long pacienteId;

    @Column(name = "medico_id")
    private Long medicoId;

    private String titulo;
    
    private String tipo;
    
    private LocalDate fecha;

    @Column(name = "archivo_url")
    private String archivoUrl;

    @Column(name = "notas_personales", columnDefinition = "TEXT")
    private String notasPersonales;

    private boolean favorito;
}
