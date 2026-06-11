package com.ejemplo.vitsync.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la API REST.
 *
 * Traduce excepciones Java a respuestas HTTP con códigos apropiados:
 * - MethodArgumentNotValidException → 400 (errores de validación de campos)
 * - BusinessException → 400 (errores de lógica de negocio)
 * - ResourceNotFoundException → 404 (recurso no encontrado)
 * - Exception genérica → 500 (error interno no controlado)
 *
 * NOTA: Ya no atrapa RuntimeException genérica, lo cual evitaba que errores
 *       internos de Spring (NPE, IllegalState, etc.) se disfrazaran como 400.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Maneja errores de validación de Bean Validation (@Valid).
     * Devuelve un mapa campo → mensaje de error.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> response = new HashMap<>();
        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", 400);
        response.put("error", "Error de validación");
        response.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja credenciales inválidas → 401 con mensaje genérico.
     * El mensaje es idéntico exista o no el usuario (anti-enumeración).
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 401);
        error.put("error", "No autorizado");
        error.put("message", "Credenciales inválidas");

        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Maneja errores de lógica de negocio (ej: NIF duplicado, contraseña incorrecta).
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 400);
        error.put("error", "Error de negocio");
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Maneja accesos denegados (IDOR, @PreAuthorize) → 403.
     * No revela si el recurso existe: mensaje genérico (anti-enumeración).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            org.springframework.security.access.AccessDeniedException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 403);
        error.put("error", "Acceso denegado");
        error.put("message", "No tienes permiso para acceder a este recurso");

        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    /**
     * Maneja recursos no encontrados (ej: buscar usuario por ID inexistente).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 404);
        error.put("error", "Recurso no encontrado");
        error.put("message", ex.getMessage());

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Violaciones de integridad (unicidad, FK) → 409 Conflict.
     * Mensaje genérico: no exponemos el detalle SQL al cliente.
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            org.springframework.dao.DataIntegrityViolationException ex) {
        logger.warn("Violación de integridad: {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 409);
        error.put("error", "Conflicto");
        error.put("message", "La operación entra en conflicto con datos existentes");

        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Fichero subido supera el límite configurado → 413 Payload Too Large.
     */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUpload(
            org.springframework.web.multipart.MaxUploadSizeExceededException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 413);
        error.put("error", "Archivo demasiado grande");
        error.put("message", "El archivo supera el tamaño máximo permitido");

        return new ResponseEntity<>(error, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    /**
     * Errores de cifrado/descifrado → 500 sin stack trace ni detalle al
     * cliente (podría revelar información sobre el esquema criptográfico).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        // Detalle solo en el log del servidor
        logger.error("Estado ilegal (posible fallo de cifrado/clave): {}", ex.getMessage());

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 500);
        error.put("error", "Error interno del servidor");
        error.put("message", "Ha ocurrido un error inesperado. Contacte con soporte.");

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Fallback: cualquier otra excepción no controlada → 500 Internal Server Error.
     * Se loguea con nivel ERROR para investigación. Nunca expone stack trace.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Error interno no controlado: ", ex);

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now().toString());
        error.put("status", 500);
        error.put("error", "Error interno del servidor");
        error.put("message", "Ha ocurrido un error inesperado. Contacte con soporte.");

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
