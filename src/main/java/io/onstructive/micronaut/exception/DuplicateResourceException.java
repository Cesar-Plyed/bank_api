package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpStatus;

/** Recurso duplicado (p. ej. número de cuenta o usuario ya existente) → 409 (spec-05). */
public class DuplicateResourceException extends BankException {

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
