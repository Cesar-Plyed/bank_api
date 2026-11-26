package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpStatus;
import java.util.UUID;

/** Cuenta inexistente o ajena al usuario autenticado → 404 (spec-05). */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public AccountNotFoundException(UUID id) {
        this("Account not found: " + id);
    }
}
