package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpStatus;

/** Saldo insuficiente para la operación → 400 (spec-05). */
public class InsufficientFundsException extends BankException {

    public InsufficientFundsException() {
        super(HttpStatus.BAD_REQUEST, "Insufficient funds");
    }

    public InsufficientFundsException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
