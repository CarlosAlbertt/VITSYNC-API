package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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

    // WRITE_ONLY: el hash se acepta al deserializar pero NUNCA se serializa
    // en respuestas JSON (fuga crítica V01 de la auditoría)
    @NotBlank
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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

    // Campos para verificación de correo electrónico.
    // @JsonIgnore: el código nunca debe salir en respuestas (V01)
    @JsonIgnore
    @Column(name = "verification_code")
    private String verificationCode;

    // @JsonProperty para que la clave JSON sea "isVerified" (el getter booleano
    // isVerified() se serializaría como "verified", que el front no lee).
    @Column(name = "is_verified", nullable = false)
    @JsonProperty("isVerified")
    private boolean isVerified = false;

    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    /** Hash del código 2FA de un solo uso enviado por email en el login. */
    @Column(name = "two_factor_code")
    @JsonIgnore
    private String twoFactorCode;

    /** Caducidad del código 2FA (10 min). */
    @Column(name = "two_factor_code_expiry")
    @JsonIgnore
    private LocalDateTime twoFactorCodeExpiry;

    @Column(name = "suspended")
    private Boolean suspended = false;

    @Column(name = "avatar_url")
    private String avatarUrl;

    // ─── Preguntas de recuperación (Fase 2) ──────────────────────────────
    // Los enunciados se guardan en claro (el usuario debe poder verlos para
    // responder en el flujo de recuperación); las respuestas se almacenan
    // hasheadas con BCrypt y NUNCA se serializan en respuestas JSON.

    @Column(name = "security_question_1")
    private String securityQuestion1;

    @Column(name = "security_question_2")
    private String securityQuestion2;

    @Column(name = "security_answer_1")
    @JsonIgnore
    private String securityAnswer1;

    @Column(name = "security_answer_2")
    @JsonIgnore
    private String securityAnswer2;

    /** Hash (BCrypt) del código de un solo uso para resetear la contraseña olvidada. */
    @Column(name = "password_reset_code")
    @JsonIgnore
    private String passwordResetCode;

    /** Caducidad del código de reseteo (10 min). */
    @Column(name = "password_reset_code_expiry")
    @JsonIgnore
    private LocalDateTime passwordResetCodeExpiry;
}