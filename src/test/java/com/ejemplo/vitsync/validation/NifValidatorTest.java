package com.ejemplo.vitsync.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link NifValidator} (DNI/NIF/NIE control-letter check).
 */
@DisplayName("NifValidator — Spanish document control-letter validation")
class NifValidatorTest {

    private final NifValidator validator = new NifValidator();

    private boolean valid(String nif) {
        return validator.isValid(nif, null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678Z", "00000000T", "11111111H", "X1234567L", "Z1234567R"})
    @DisplayName("Valid DNI/NIE with correct control letter")
    void validNifs(String nif) {
        assertTrue(valid(nif), nif + " debería ser válido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678A", "12345678Y", "11111111A", "X1234567Z"})
    @DisplayName("Wrong control letter is rejected")
    void wrongControlLetter(String nif) {
        assertFalse(valid(nif), nif + " debería ser inválido (letra de control)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567Z", "123456789Z", "ABCDEFGHZ", "12345678", "12-345-678Z"})
    @DisplayName("Wrong format is rejected")
    void wrongFormat(String nif) {
        assertFalse(valid(nif), nif + " debería ser inválido (formato)");
    }

    @Test
    @DisplayName("null/blank are valid here (delegated to @NotBlank)")
    void nullOrBlankIsValid() {
        assertTrue(valid(null));
        assertTrue(valid(""));
        assertTrue(valid("   "));
    }

    @Test
    @DisplayName("Lower-case input is normalised and accepted")
    void lowerCaseAccepted() {
        assertTrue(valid("12345678z"));
    }
}
