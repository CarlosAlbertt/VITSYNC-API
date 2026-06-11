package com.ejemplo.vitsync.converter;

import com.ejemplo.vitsync.config.SensitiveDataKeyHolder;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA attribute converter that transparently encrypts/decrypts a String
 * column with <b>AES-256-GCM</b> (authenticated encryption).
 *
 * <p>Applied to special-category health fields (GDPR Art. 9) so they are
 * stored ciphertext at rest (Art. 32). Usage:
 * {@code @Convert(converter = SensitiveDataConverter.class)} on the field.</p>
 *
 * <h3>Algorithm choice</h3>
 * <ul>
 *   <li><b>AES-256</b>: symmetric, fast, FIPS-approved; 256-bit key.</li>
 *   <li><b>GCM mode</b>: provides confidentiality AND integrity (a 128-bit
 *       authentication tag). Tampered ciphertext fails to decrypt instead of
 *       yielding garbage — unlike CBC, which is also padding-oracle prone.</li>
 *   <li>No external libraries: only {@code javax.crypto}.</li>
 * </ul>
 *
 * <h3>Stored format</h3>
 * <p>{@code base64( IV(12 bytes) || ciphertext+tag )}. A fresh random 12-byte
 * IV is generated per value (GCM's nonce must never repeat under the same
 * key), so encrypting the same plaintext twice yields different ciphertexts.</p>
 *
 * <h3>Consequences</h3>
 * <p>Encrypted columns CANNOT be indexed meaningfully nor used in SQL
 * {@code WHERE}/{@code LIKE}/{@code ORDER BY}: the database only sees opaque
 * base64. Any search over these fields must load and decrypt in the
 * application. Choose which fields to encrypt accordingly.</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Converter
public class SensitiveDataConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;     // 96 bits, recomendado para GCM
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypts the entity attribute before it is written to the database.
     *
     * @param attribute plaintext (may be {@code null})
     * @return base64 of {@code IV || ciphertext}, or {@code null} if input
     *         was {@code null}
     * @throws IllegalStateException if encryption fails (key/algorithm issue)
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        // null se mantiene como null en BD: no ciframos la ausencia de dato
        if (attribute == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, SensitiveDataKeyHolder.getKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

            // Prefijamos el IV (no es secreto) para poder descifrar después
            byte[] combined = ByteBuffer.allocate(iv.length + cipherText.length)
                    .put(iv).put(cipherText).array();
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // No incluimos el texto en claro ni detalle criptográfico en el mensaje
            throw new IllegalStateException("Fallo al cifrar dato sensible", e);
        }
    }

    /**
     * Decrypts the database value back into the entity attribute.
     *
     * @param dbData base64 of {@code IV || ciphertext} (may be {@code null})
     * @return plaintext, or {@code null} if input was {@code null}
     * @throws IllegalStateException if decryption/authentication fails
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbData);
            ByteBuffer buffer = ByteBuffer.wrap(combined);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherText = new byte[buffer.remaining()];
            buffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, SensitiveDataKeyHolder.getKey(),
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Falla también si el tag GCM no valida (dato manipulado en BD)
            throw new IllegalStateException("Fallo al descifrar dato sensible", e);
        }
    }
}
