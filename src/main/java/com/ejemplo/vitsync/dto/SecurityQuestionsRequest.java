package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code POST /VitSync-app/api/users/{id}/security/questions}.
 *
 * <p>Field names ({@code q1/a1/q2/a2}) match the payload already sent by the
 * frontend. The two answers are stored hashed with BCrypt server-side; they
 * are never returned in any response.</p>
 */
@Data
public class SecurityQuestionsRequest {

    @NotBlank(message = "La primera pregunta es obligatoria")
    @Size(max = 200, message = "La pregunta es demasiado larga")
    private String q1;

    @NotBlank(message = "La respuesta a la primera pregunta es obligatoria")
    @Size(max = 200, message = "La respuesta es demasiado larga")
    private String a1;

    @NotBlank(message = "La segunda pregunta es obligatoria")
    @Size(max = 200, message = "La pregunta es demasiado larga")
    private String q2;

    @NotBlank(message = "La respuesta a la segunda pregunta es obligatoria")
    @Size(max = 200, message = "La respuesta es demasiado larga")
    private String a2;
}
