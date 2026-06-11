package com.ejemplo.vitsync.config;

import javax.crypto.SecretKey;

/**
 * Static holder bridging the Spring-managed {@link EncryptionConfig} and the
 * Hibernate-instantiated {@code SensitiveDataConverter}.
 *
 * <p>Not a general-purpose registry: it holds exactly one key, set once at
 * boot. Tests may call {@link #setKey} directly with an ad-hoc key.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
public final class SensitiveDataKeyHolder {

    private static volatile SecretKey key;

    private SensitiveDataKeyHolder() {
    }

    /**
     * Publishes the AES-256 key (called once from {@link EncryptionConfig}).
     *
     * @param secretKey 256-bit AES key
     */
    public static void setKey(SecretKey secretKey) {
        key = secretKey;
    }

    /**
     * @return the AES-256 key for field-level encryption
     * @throws IllegalStateException if called before the key was configured
     */
    public static SecretKey getKey() {
        SecretKey current = key;
        if (current == null) {
            throw new IllegalStateException(
                    "Clave de cifrado no inicializada: revisa ENCRYPTION_KEY y EncryptionConfig");
        }
        return current;
    }
}
