package com.ejemplo.vitsync.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for {@code POST /api/auth/refresh} and
 * {@code POST /api/auth/logout}.
 *
 * <p>Validation: the token is opaque base64url (43 chars for 256 bits);
 * the length cap merely blocks abusive payloads.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequest {

    @NotBlank(message = "El refresh token es obligatorio")
    @Size(max = 128, message = "Refresh token inválido")
    private String refreshToken;
}
