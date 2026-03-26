package com.ejemplo.vitsync.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pacientes")
public class Paciente extends User {

    @Column(name = "historial_clinico_id")
    private String historialClinicoId;

    @Column(name = "grupo_sanguineo")
    private String grupoSanguineo;

    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    @Column(name = "condiciones_previas", columnDefinition = "TEXT")
    private String condicionesPrevias;

    @Column(name = "contacto_emergencia")
    private String contactoEmergencia;
}
