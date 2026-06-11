package com.ejemplo.vitsync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Loads the AES-256 data-at-rest encryption key from the environment and
 * hands it to {@code SensitiveDataConverter} through a static holder.
 *
 * <p>Why a static holder: JPA attribute converters are instantiated by
 * Hibernate, not by Spring, so constructor injection is not reliable across
 * Hibernate versions. This config class IS a Spring bean, validates the key
 * at boot (fail-fast) and publishes it for the converter.</p>
 *
 * <p>Key requirements: 32 bytes (256 bits) base64-encoded, provided via the
 * {@code ENCRYPTION_KEY} environment variable. Generate with
 * {@code openssl rand -base64 32}.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Configuration
public class EncryptionConfig {

    /**
     * Validates and publishes the AES key at startup.
     *
     * @param encodedKey base64 of exactly 32 random bytes
     * @throws IllegalStateException if the key is missing or not 32 bytes
     */
    public EncryptionConfig(@Value("${vitsync.encryption.key}") String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("ENCRYPTION_KEY no es base64 válido", e);
        }
        if (keyBytes.length != 32) {
            // AES-256 exige exactamente 32 bytes: ni truncamos ni derivamos,
            // una clave debilitada silenciosamente sería peor que fallar
            throw new IllegalStateException(
                    "ENCRYPTION_KEY debe ser 32 bytes en base64 (actual: " + keyBytes.length + " bytes)");
        }
        SensitiveDataKeyHolder.setKey(new SecretKeySpec(keyBytes, "AES"));
    }
}
