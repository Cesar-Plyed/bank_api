package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpStatus;

/** Violación de una regla de negocio → 400 (spec-05). */
public class BusinessRuleException extends BankException {

    public BusinessRuleException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
