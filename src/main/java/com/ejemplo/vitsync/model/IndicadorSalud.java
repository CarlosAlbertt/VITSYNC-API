package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.CategoriaSalud;
import com.ejemplo.vitsync.enums.EstadoIndicador;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "indicadores_salud")
public class IndicadorSalud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "paciente_id", nullable = false)
    private Long pacienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaSalud categoria;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Double valor;

    private String unidad;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoIndicador estado;
}
