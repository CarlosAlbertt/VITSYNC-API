package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data // GENERA GETTERS, SETTER, TOSTRING , EQUALS Y HASHCODE de forma automatica
@AllArgsConstructor // GENERA CONSTRUCTOR CON TODOS LOS ATRIBUTOS
@NoArgsConstructor // GENERA CONSTRUCTOR VACIO
@Table(name = "Users") // NOMBRE DE LA TABLA EN LA BASE DE DATOS
@Entity // INDICA QUE ES UNA ENTIDAD DE JPA
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id // INDICA QUE ES LA CLAVE PRIMARIA POR LO TANTO SE GENERA AUTOMÁTICAMENTE EN LA
    // BASE DE DATOS
    @GeneratedValue(strategy = GenerationType.IDENTITY) // INDICA QUE EL VALOR SE GENERA AUTOMÁTICAMENTE
    private Long id;

    @NotBlank // INDICA QUE NO PUEDE SER NULO NI VACIO
    private String name;

    @NotBlank
    private String firstName;

    @NotBlank
    private String secondName;

    @NotBlank
    @Column(unique = true)
    private String nif;

    @NotBlank
    @Column(unique = true)
    private String email;

    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING) // INDICA QUE ES UN ENUMERADO Y SE ALMACENA COMO CADENA DE TEXTO
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Fecha de nacimiento del usuario — almacenada como LocalDate para consultas temporales */
    @Column(nullable = false)
    private LocalDate birthDate;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String postCode;

    @NotBlank
    private String country;

    // Campos para verificación de correo electrónico
    @Column(name = "verification_code")
    private String verificationCode;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Column(name = "suspended")
    private Boolean suspended = false;

    @Column(name = "avatar_url")
    private String avatarUrl;
}