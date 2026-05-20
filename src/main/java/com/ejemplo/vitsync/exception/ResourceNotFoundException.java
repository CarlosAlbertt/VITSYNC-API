package com.ejemplo.vitsync.exception;

/**
 * Excepción lanzada cuando un recurso solicitado no se encuentra en la base de datos.
 * Se traduce automáticamente a HTTP 404 Not Found por el GlobalExceptionHandler.
 *
 * Uso: throw new ResourceNotFoundException("Usuario no encontrado con id: " + id);
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
