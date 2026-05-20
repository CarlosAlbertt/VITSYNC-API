package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MedicoRequest {

    // ── Campos heredados de User (obligatorios) ──────────────────────────
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El primer apellido es obligatorio")
    private String firstName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    private String secondName;

    @NotBlank(message = "El NIF/CIF es obligatorio")
    private String nif;

    @NotBlank(message = "El email es obligatorio")
    private String email;

    // En creación es obligatoria; en actualización es opcional (si null se conserva la actual)
    private String password;

    @NotNull(message = "El género es obligatorio")
    private com.ejemplo.vitsync.enums.Gender gender;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "El código postal es obligatorio")
    private String postCode;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    // ── Campos específicos de Médico ──────────────────────────────────────
    @NotBlank(message = "El número de colegiado es obligatorio")
    private String numeroColegiado;

    private String fotoUrl;
    private String bio;

    // ID de la especialidad asignada (puede ser null si no se asigna)
    private Long especialidadId;

    // Estado activo (por defecto true si no se envía)
    private Boolean activo;
}
