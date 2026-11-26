package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpStatus;

/**
 * Base de las excepciones de dominio (spec-05).
 *
 * <p>Cada excepción de dominio lleva asociado su status HTTP para que el
 * {@link GlobalExceptionHandler} pueda traducirla de forma centralizada.
 */
public abstract class BankException extends RuntimeException {

    private final HttpStatus status;

    protected BankException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    protected BankException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
