package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import com.ejemplo.vitsync.validation.ValidNif;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Request body for {@code PUT /api/usuarios/{id}} (admin user update).
 *
 * <p>Same validation profile as registration except the password is optional
 * (only re-hashed when present). The role field is honoured only because this
 * endpoint is ADMIN-restricted in {@code SecurityConfig}.</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]+$", message = "El nombre solo puede contener letras, espacios y guiones")
    private String name;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Size(max = 100, message = "El primer apellido no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]+$", message = "El primer apellido solo puede contener letras, espacios y guiones")
    private String firstName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    @Size(max = 100, message = "El segundo apellido no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]+$", message = "El segundo apellido solo puede contener letras, espacios y guiones")
    private String secondName;

    @NotBlank(message = "El NIF es obligatorio")
    @ValidNif
    private String nif;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    @Size(max = 254, message = "El email no puede superar 254 caracteres")
    private String email;

    @NotNull(message = "El género es obligatorio")
    private Gender gender;

    // El rol solo lo puede cambiar un admin (endpoint restringido a ADMIN)
    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^(\\+34|0034|34)?[6789]\\d{8}$", message = "El teléfono debe ser un número español válido")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    private String address;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String postCode;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 100, message = "El país no puede superar 100 caracteres")
    private String country;

    // Contraseña opcional: si se envía se hashea y actualiza; si no, se deja la actual.
    // Cuando viene, debe cumplir la política fuerte (≥12, mayús/minús/núm/especial).
    @Pattern(
            regexp = "^$|^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,}$",
            message = "La contraseña debe tener al menos 12 caracteres, con mayúscula, minúscula, número y carácter especial")
    private String password;
}
