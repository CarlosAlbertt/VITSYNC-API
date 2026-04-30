package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.EstadoIndicador;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class IndicadorSaludRequest {

    @NotBlank
    private String nombre;

    @NotNull
    private Double valor;

    private String unidad;

    /** Si no se envía, el servicio usa la fecha actual. */
    private LocalDate fecha;

    @NotNull
    private EstadoIndicador estado;
}
