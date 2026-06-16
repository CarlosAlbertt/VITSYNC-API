package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.Map;

@Data
public class CitaRequest {
    private Map<String, Object> location;

    @Size(max = 100, message = "La especialidad no puede superar los 100 caracteres")
    private String specialty;

    private Map<String, Object> doctor;

    @NotBlank(message = "La fecha es obligatoria")
    private String date;

    @NotBlank(message = "La hora es obligatoria")
    private String time;

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String reason;
}
