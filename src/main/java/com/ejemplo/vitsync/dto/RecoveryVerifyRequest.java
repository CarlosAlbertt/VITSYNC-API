package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Step 2 of the forgotten-password flow: the user answers their two security
 * questions ({@code POST /api/auth/recovery/verify}). If both answers are
 * correct, the API emails a one-time reset code.
 */
@Data
public class RecoveryVerifyRequest {

    @NotBlank(message = "El NIF/CIF es obligatorio")
    @Size(max = 20, message = "NIF/CIF no válido")
    private String nif;

    @NotBlank(message = "La respuesta a la primera pregunta es obligatoria")
    @Size(max = 200, message = "La respuesta es demasiado larga")
    private String answer1;

    @NotBlank(message = "La respuesta a la segunda pregunta es obligatoria")
    @Size(max = 200, message = "La respuesta es demasiado larga")
    private String answer2;
}
