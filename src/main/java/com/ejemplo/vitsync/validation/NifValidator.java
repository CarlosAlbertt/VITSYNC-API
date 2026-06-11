package com.ejemplo.vitsync.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validates a Spanish DNI/NIF (and NIE) including its control letter.
 *
 * <p>Algorithm: take the 8-digit number modulo 23 and map the remainder to a
 * letter via the official table {@code TRWAGMYFPDXBNJZSQVHLCKE}. For a NIE the
 * leading {@code X/Y/Z} is replaced by {@code 0/1/2} before the modulo.</p>
 *
 * <p>{@code null}/blank are considered valid here so the constraint composes
 * with {@code @NotBlank} (which owns the "required" check).</p>
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
public class NifValidator implements ConstraintValidator<ValidNif, String> {

    private static final String CONTROL_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Delegamos la obligatoriedad a @NotBlank: aquí null/blank = válido
        if (value == null || value.isBlank()) {
            return true;
        }

        String nif = value.trim().toUpperCase();
        if (!nif.matches("^[XYZ]?\\d{7,8}[A-Z]$")) {
            return false;
        }

        // NIE: sustituir la inicial X/Y/Z por 0/1/2 para el cálculo
        String numberPart = nif.substring(0, nif.length() - 1);
        char controlChar = nif.charAt(nif.length() - 1);

        if (numberPart.startsWith("X")) {
            numberPart = "0" + numberPart.substring(1);
        } else if (numberPart.startsWith("Y")) {
            numberPart = "1" + numberPart.substring(1);
        } else if (numberPart.startsWith("Z")) {
            numberPart = "2" + numberPart.substring(1);
        }

        try {
            int number = Integer.parseInt(numberPart);
            char expected = CONTROL_LETTERS.charAt(number % 23);
            return expected == controlChar;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
