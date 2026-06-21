package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Step 3 of the forgotten-password flow: the user submits the emailed code
 * plus the new password ({@code POST /api/auth/recovery/reset}).
 *
 * <p>The new password is validated against the same health-grade policy as
 * registration and password change (≥12 chars with upper, lower, digit and
 * special char).</p>
 */
@Data
public class RecoveryResetRequest {

    @NotBlank(message = "El NIF/CIF es obligatorio")
    @Size(max = 20, message = "NIF/CIF no válido")
    private String nif;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 10, message = "Código no válido")
    private String code;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$",
            message = "La contraseña debe tener al menos 12 caracteres, con mayúscula, minúscula, número y carácter especial")
    private String newPassword;
}
