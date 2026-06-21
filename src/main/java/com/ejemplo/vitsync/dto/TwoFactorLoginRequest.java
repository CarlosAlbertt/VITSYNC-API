package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Second step of a 2FA login: the NIF from the first step plus the 6-digit
 * code received by email.
 */
@Data
public class TwoFactorLoginRequest {

    @NotBlank(message = "El NIF es obligatorio")
    private String nif;

    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "^\\d{6}$", message = "El código debe tener 6 dígitos")
    private String code;
}
