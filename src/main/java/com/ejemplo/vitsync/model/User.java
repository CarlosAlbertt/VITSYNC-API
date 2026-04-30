package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "Users")
@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @NotBlank
    private String birthDate;

    @NotBlank
    private String phone;

    @NotBlank
    private String address;

    @NotBlank
    private String postCode;

    @NotBlank
    private String country;

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

    @Column(name = "security_q1")
    private String securityQuestion1;

    @Column(name = "security_a1")
    private String securityAnswer1;

    @Column(name = "security_q2")
    private String securityQuestion2;

    @Column(name = "security_a2")
    private String securityAnswer2;
}