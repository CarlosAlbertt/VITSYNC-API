package com.ejemplo.vitsync.converter;

import com.ejemplo.vitsync.config.SensitiveDataKeyHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link SensitiveDataConverter} (AES-256-GCM).
 */
@DisplayName("SensitiveDataConverter — AES-256-GCM field encryption")
class SensitiveDataConverterTest {

    private final SensitiveDataConverter converter = new SensitiveDataConverter();

    @BeforeAll
    static void initKey() {
        byte[] key = Base64.getDecoder().decode("EKvIieNGOCESKX7qut8o8zXkxw+3WNFY3VUGdpFI+wc=");
        SensitiveDataKeyHolder.setKey(new SecretKeySpec(key, "AES"));
    }

    @Test
    @DisplayName("Round-trip returns the original plaintext")
    void roundTrip_returnsOriginal() {
        String plain = "Alergia a la penicilina";
        String encrypted = converter.convertToDatabaseColumn(plain);
        assertNotEquals(plain, encrypted);
        assertEquals(plain, converter.convertToEntityAttribute(encrypted));
    }

    @Test
    @DisplayName("null is handled without exception (both directions)")
    void null_isHandled() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    @DisplayName("Same plaintext encrypts to different ciphertexts (random IV)")
    void sameInput_differentCiphertext() {
        String plain = "Grupo sanguíneo A+";
        String c1 = converter.convertToDatabaseColumn(plain);
        String c2 = converter.convertToDatabaseColumn(plain);
        assertNotEquals(c1, c2, "El IV aleatorio debe producir ciphertexts distintos");
        // Pero ambos descifran al mismo valor
        assertEquals(plain, converter.convertToEntityAttribute(c1));
        assertEquals(plain, converter.convertToEntityAttribute(c2));
    }

    @Test
    @DisplayName("Unicode and special characters survive the round-trip")
    void unicodeAndSpecialChars_survive() {
        String plain = "Diagnóstico: ñoño 😷 <script>&\"'\t\n— €";
        assertEquals(plain, converter.convertToEntityAttribute(
                converter.convertToDatabaseColumn(plain)));
    }

    @Test
    @DisplayName("Empty string round-trips correctly")
    void emptyString_roundTrips() {
        assertEquals("", converter.convertToEntityAttribute(
                converter.convertToDatabaseColumn("")));
    }

    @Test
    @DisplayName("Tampered ciphertext fails to decrypt (GCM auth tag)")
    void tampered_failsToDecrypt() {
        String encrypted = converter.convertToDatabaseColumn("dato sensible");
        // Alterar un byte del ciphertext
        char[] chars = encrypted.toCharArray();
        chars[chars.length - 2] = (chars[chars.length - 2] == 'A') ? 'B' : 'A';
        String tampered = new String(chars);
        assertThrows(IllegalStateException.class,
                () -> converter.convertToEntityAttribute(tampered));
    }
}
