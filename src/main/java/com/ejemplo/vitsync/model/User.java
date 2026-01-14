package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.enums.Gender;
import com.ejemplo.vitsync.enums.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data // GENERA GETTERS, SETTER, TOSTRING , EQUALS Y HASHCODE de forma automatica
@AllArgsConstructor // GENERA CONSTRUCTOR CON TODOS LOS ATRIBUTOS
@NoArgsConstructor // GENERA CONSTRUCTOR VACIO
@Table(name = "Users") // NOMBRE DE LA TABLA EN LA BASE DE DATOS
@Entity // INDICA QUE ES UNA ENTIDAD DE JPA
public class User {

    @Id // INDICA QUE ES LA CLAVE PRIMARIA POR LO TANTO SE GENERA AUTOMÁTICAMENTE EN LA
        // BASE DE DATOS
    @GeneratedValue(strategy = GenerationType.IDENTITY) // INDICA QUE EL VALOR SE GENERA AUTOMÁTICAMENTE
    private Long id;

    @NotBlank // INDICA QUE NO PUEDE SER NULO NI VACIO
    private String name;

    @NotBlank
    private String secondName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String username;

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

    // Relación: Un usuario PROFESIONAL pertenece a una especialidad
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;
}