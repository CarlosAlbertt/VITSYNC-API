package com.ejemplo.vitsync.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Bean Validation constraint for Spanish DNI/NIF documents that checks not
 * only the format ({@code 8 digits + letter}) but also the <b>control
 * letter</b> (mod-23 algorithm). This catches typos that a pure regex would
 * accept (audit recommendation for finding around input validation).
 *
 * @author VitSync Team
 * @version 1.0
 * @since 2.0
 */
@Documented
@Constraint(validatedBy = NifValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface ValidNif {

    /** @return validation message when the NIF/letter is invalid */
    String message() default "El NIF no es válido (formato u carácter de control incorrecto)";

    /** @return validation groups */
    Class<?>[] groups() default {};

    /** @return payload */
    Class<? extends Payload>[] payload() default {};
}
