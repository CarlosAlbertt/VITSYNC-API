package com.ejemplo.vitsync.dto;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El primer apellido es obligatorio")
    private String firstName;

    @NotBlank(message = "El segundo apellido es obligatorio")
    private String secondName;

    @NotBlank(message = "El NIF/CIF es obligatorio")
    private String nif;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email debe ser válido")
    private String email;

    @NotNull(message = "El género es obligatorio")
    private Gender gender;

    // El rol solo lo puede cambiar un admin
    @NotNull(message = "El rol es obligatorio")
    private Role role;

    @NotBlank(message = "La fecha de nacimiento es obligatoria")
    private String birthDate;

    @NotBlank(message = "El teléfono es obligatorio")
    private String phone;

    @NotBlank(message = "La dirección es obligatoria")
    private String address;

    @NotBlank(message = "El código postal es obligatorio")
    private String postCode;

    @NotBlank(message = "El país es obligatorio")
    private String country;

    // Contraseña opcional: si se envía se hashea y actualiza; si no, se deja la actual
    private String password;
}
