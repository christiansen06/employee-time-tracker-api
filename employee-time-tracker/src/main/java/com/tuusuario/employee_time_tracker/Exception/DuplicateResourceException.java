package com.tuusuario.employee_time_tracker.Exception;

/**
 * Se lanza cuando se intenta crear un recurso que ya existe
 * (por ejemplo, un username repetido). Se traduce a HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
