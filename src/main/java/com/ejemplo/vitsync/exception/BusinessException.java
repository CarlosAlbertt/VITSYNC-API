package com.ejemplo.vitsync.exception;

/**
 * Excepción lanzada para errores de lógica de negocio (validaciones, reglas de negocio).
 * Se traduce automáticamente a HTTP 400 Bad Request por el GlobalExceptionHandler.
 *
 * Uso: throw new BusinessException("El NIF ya está registrado");
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
