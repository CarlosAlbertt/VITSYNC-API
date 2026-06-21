package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Step 1 of the forgotten-password flow: the user identifies themselves by
 * NIF to retrieve their two security questions
 * ({@code POST /api/auth/recovery/questions}).
 */
@Data
public class RecoveryStartRequest {

    @NotBlank(message = "El NIF/CIF es obligatorio")
    @Size(max = 20, message = "NIF/CIF no válido")
    private String nif;
}
