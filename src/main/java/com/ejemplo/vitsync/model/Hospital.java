package com.ejemplo.vitsync.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que representa un hospital o centro médico del sistema.
 *
 * Se usa @Column(name=...) para mantener compatibilidad con la BD existente
 * mientras los campos Java siguen la convención en español del proyecto.
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "hospitales")
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del hospital o centro médico */
    @Column(name = "name")
    private String nombre;

    /** Dirección física del centro */
    @Column(name = "address")
    private String direccion;

    /** URL de la imagen del centro */
    @Column(name = "image")
    private String imagen;

    /** Teléfono de contacto */
    @Column(name = "phone")
    private String telefono;
}
