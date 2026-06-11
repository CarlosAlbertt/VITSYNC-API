package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.converter.SensitiveDataConverter;
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

    /**
     * Notas personales/clínicas sobre el informe.
     * Dato clínico (Art. 9 RGPD): cifrado en reposo con AES-256-GCM.
     */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "notas_personales", columnDefinition = "TEXT")
    private String notasPersonales;

    /** Si el paciente ha marcado este informe como favorito */
    private boolean favorito;
}
