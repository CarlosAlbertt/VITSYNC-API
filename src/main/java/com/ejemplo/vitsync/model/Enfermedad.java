package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo de enfermedades y sus tratamientos asociados.
 *
 * <p>La especialidad relacionada se guarda como texto libre
 * ({@code especialidadRelacionada}) para casar con el contrato del frontend
 * (no es una clave foránea a {@link Especialidad}). Los tratamientos son una
 * lista de cadenas persistida en una tabla secundaria
 * ({@code enfermedad_tratamientos}) mediante {@code @ElementCollection}.</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "enfermedades")
@Entity
public class Enfermedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    // EAGER: la lista es pequeña y se serializa en las respuestas REST; evita
    // LazyInitializationException al devolver la entidad fuera de la transacción.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "enfermedad_tratamientos",
            joinColumns = @JoinColumn(name = "enfermedad_id"))
    @Column(name = "tratamiento")
    private List<String> tratamientos = new ArrayList<>();

    @Column(name = "especialidad_relacionada")
    private String especialidadRelacionada;

    @Column(nullable = false)
    private Boolean activo = true;
}
