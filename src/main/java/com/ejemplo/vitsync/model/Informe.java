package com.ejemplo.vitsync.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

/**
 * Entidad que representa un informe médico del sistema.
 *
 * Relaciones JPA:
 * - Cada informe pertenece a un Paciente (@ManyToOne)
 * - Cada informe es emitido por un Médico (@ManyToOne)
 *
 * Tipos: "Laboratorio", "Diagnóstico", "Imagen", "Receta electrónica"
 */
@Data
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "informes")
public class Informe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Paciente al que pertenece el informe */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Paciente paciente;

    /** Médico que emitió el informe */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "password", "verificationCode"})
    private Medico medico;

    /** Título descriptivo del informe */
    private String titulo;

    /** Tipo de informe: Laboratorio, Diagnóstico, Imagen, Receta electrónica */
    private String tipo;

    /** Fecha de emisión del informe */
    private LocalDate fecha;

    /** URL del archivo adjunto (PDF, imagen, etc.) */
    @Column(name = "archivo_url")
    private String archivoUrl;

    /** Notas personales del paciente sobre el informe */
    @Column(name = "notas_personales", columnDefinition = "TEXT")
    private String notasPersonales;

    /** Si el paciente ha marcado este informe como favorito */
    private boolean favorito;
}
