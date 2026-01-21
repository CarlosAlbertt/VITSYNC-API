package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El nombre de usuario es obligatorio")
    private String nif;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;
}
