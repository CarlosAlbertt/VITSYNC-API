package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response body for authentication endpoints
 * ({@code /api/auth/login}, {@code /api/auth/register}, {@code /api/auth/refresh}).
 *
 * <p>{@code token} is the short-lived (15 min) RS256 access token — the
 * frontend should keep it in JS memory only. {@code refreshToken} is the
 * opaque 7-day token — the frontend MUST store it in an httpOnly cookie,
 * never in localStorage (see README, "Seguridad y Autenticación").</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthResponse {

    /** Short-lived RS256 access token (15 min). */
    private String token;

    /**
     * Opaque revocable refresh token (7 days). Lo usa el controlador SOLO para
     * fijar la cookie httpOnly; nunca se serializa en el cuerpo de la respuesta
     * ({@code @JsonIgnore}) para que JavaScript no pueda leerlo (única vía segura
     * = cookie httpOnly).
     */
    @JsonIgnore
    private String refreshToken;

    private String nif;
    private Long id;
    private String email;
    private Role role;
    private String message;

    /** true cuando el login requiere un segundo paso (código 2FA por email). */
    private boolean twoFactorRequired;
}
