package com.ejemplo.vitsync.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    // Se serializan con las claves que espera el frontend (name/address/image/phone).
    // El frontend (booking) lee location.name / location.address; sin esto llegaban
    // como nombre/direccion y salían "undefined" (el nombre del hospital no aparecía).

    /** Nombre del hospital o centro médico */
    @Column(name = "name")
    @JsonProperty("name")
    private String nombre;

    /** Dirección física del centro */
    @Column(name = "address")
    @JsonProperty("address")
    private String direccion;

    /** URL de la imagen del centro */
    @Column(name = "image")
    @JsonProperty("image")
    private String imagen;

    /** Teléfono de contacto */
    @Column(name = "phone")
    @JsonProperty("phone")
    private String telefono;
}
