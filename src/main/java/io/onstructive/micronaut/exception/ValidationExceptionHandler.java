package io.onstructive.micronaut.exception;

import io.micronaut.context.annotation.Replaces;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import io.micronaut.validation.exceptions.ConstraintExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolationException;

/**
 * Sustituye al handler de validación que trae Micronaut para que las violaciones
 * de {@code @Valid} usen el payload único con {@code fieldErrors} (spec-05).
 */
@Produces
@Singleton
@Replaces(ConstraintExceptionHandler.class)
public class ValidationExceptionHandler
        implements ExceptionHandler<ConstraintViolationException, HttpResponse<?>> {

    @Override
    public HttpResponse<?> handle(HttpRequest request, ConstraintViolationException exception) {
        return GlobalExceptionHandler.validationResponse(request, exception);
    }
}
