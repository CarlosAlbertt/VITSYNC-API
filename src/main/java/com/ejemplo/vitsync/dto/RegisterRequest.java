package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo puede contener letras")
    private String name;

    @NotBlank(message = "El primer apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El primer apellido solo puede contener letras")
    private String firstName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El segundo apellido solo puede contener letras")
    private String secondName;

    @NotBlank(message = "El NIF/CIF es obligatorio")
    @Pattern(regexp = "^[XYZ]?\\d{5,8}[A-Z]$|^[A-HJ-NP-SV-W]\\d{7}[0-9A-J]$", message = "El formato del documento no es un NIF, NIE ni CIF válido")
    private String nif;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$", message = "La contraseña debe tener al menos 8 caracteres, conteniendo letras y números")
    private String password;

    @NotNull(message = "El género es obligatorio")
    private Gender gender;

    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    private LocalDate birthDate;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^\\+?[\\d\\s-]{9,15}$", message = "El formato del teléfono es inválido")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String postCode;

    @NotBlank(message = "El país es obligatorio")
    private String country;
}
