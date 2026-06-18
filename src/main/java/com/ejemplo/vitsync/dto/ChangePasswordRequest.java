package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Request body for {@code PATCH /VitSync-app/api/users/{id}/password}.
 *
 * The new password is validated against the same health-grade policy as
 * registration (≥12 chars with upper, lower, digit and special char). The
 * current password is verified server-side before applying the change.
 */
@Data
public class ChangePasswordRequest {

    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$",
            message = "La contraseña debe tener al menos 12 caracteres, con mayúscula, minúscula, número y carácter especial")
    private String newPassword;
}
