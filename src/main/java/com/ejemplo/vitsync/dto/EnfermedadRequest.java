package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * Cuerpo de petición para crear/actualizar una enfermedad
 * ({@code POST/PUT /api/enfermedades}). Refleja el contrato del frontend.
 */
@Data
public class EnfermedadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 255, message = "El nombre no puede superar 255 caracteres")
    private String nombre;

    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String descripcion;

    @NotEmpty(message = "Añade al menos un tratamiento")
    private List<@NotBlank @Size(max = 255) String> tratamientos;

    @NotBlank(message = "La especialidad relacionada es obligatoria")
    @Size(max = 255, message = "La especialidad no puede superar 255 caracteres")
    private String especialidadRelacionada;

    // Opcional: por defecto activa al crear
    private Boolean activo;
}
