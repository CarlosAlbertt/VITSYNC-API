package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Gender;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for {@code PUT /VitSync-app/api/users/{id}/profile}
 * (self-service profile edit).
 *
 * <p>All fields are optional (partial update): only non-null values are
 * applied by the service. Replaces the previous untyped {@code Map<String,
 * Object>} which had no validation and allowed unchecked casts (audit
 * findings around input validation). It deliberately omits {@code nif},
 * {@code email}, {@code role}, {@code password} and verification flags: those
 * are not editable through self-service.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Data
public class ProfileUpdateRequest {

    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]*$", message = "El nombre solo puede contener letras, espacios y guiones")
    private String name;

    @Size(max = 100, message = "El primer apellido no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]*$", message = "El primer apellido solo puede contener letras, espacios y guiones")
    private String firstName;

    @Size(max = 100, message = "El segundo apellido no puede superar 100 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s-]*$", message = "El segundo apellido solo puede contener letras, espacios y guiones")
    private String secondName;

    private Gender gender;

    @Pattern(regexp = "^$|^(\\+34|0034|34)?[6789]\\d{8}$", message = "El teléfono debe ser un número español válido")
    private String phone;

    @Size(max = 200, message = "La dirección no puede superar 200 caracteres")
    private String address;

    @Pattern(regexp = "^$|^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String postCode;

    @Size(max = 100, message = "El país no puede superar 100 caracteres")
    private String country;
}
