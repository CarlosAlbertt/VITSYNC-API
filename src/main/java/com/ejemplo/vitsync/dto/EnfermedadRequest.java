package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EnfermedadRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String descripcion;

    @NotNull(message = "Los tratamientos son obligatorios")
    private List<String> tratamientos;

    @NotBlank(message = "La especialidad relacionada es obligatoria")
    private String especialidadRelacionada;

    private Boolean activo;
}
