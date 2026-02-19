package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EspecialidadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotBlank(message = "El código es obligatorio")
    private String codigo;

    private String descripcion;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo; // MEDICA, QUIRURGICA, DIAGNOSTICO, GENERAL, UNIDAD

    private String slug;

    private String icono;

    private Boolean activo;
}
