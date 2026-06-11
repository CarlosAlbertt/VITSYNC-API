package com.ejemplo.vitsync.model;

import com.ejemplo.vitsync.converter.SensitiveDataConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Patient entity (JOINED subtype of {@link User}).
 *
 * <p>The clinical fields below are special-category data (GDPR Art. 9) and
 * are encrypted at rest with {@link SensitiveDataConverter} (AES-256-GCM).
 * Because the stored values are ciphertext, these columns cannot be indexed
 * or queried with SQL {@code WHERE}/{@code LIKE} — filtering must happen in
 * the application after decryption.</p>
 *
 * @author VitSync Team
 * @version 2.0
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "pacientes")
public class Paciente extends User {

    /** Clinical-history external identifier. Encrypted at rest. */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "historial_clinico_id")
    private String historialClinicoId;

    /** Blood group (e.g. "A+"). Encrypted at rest. */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "grupo_sanguineo")
    private String grupoSanguineo;

    /** Free-text allergies. Encrypted at rest. */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "alergias", columnDefinition = "TEXT")
    private String alergias;

    /** Pre-existing medical conditions. Encrypted at rest. */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "condiciones_previas", columnDefinition = "TEXT")
    private String condicionesPrevias;

    /** Emergency contact (may include medical context). Encrypted at rest. */
    @Convert(converter = SensitiveDataConverter.class)
    @Column(name = "contacto_emergencia", columnDefinition = "TEXT")
    private String contactoEmergencia;
}
