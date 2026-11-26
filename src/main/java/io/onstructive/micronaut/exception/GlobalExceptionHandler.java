package io.onstructive.micronaut.exception;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manejo centralizado de errores de {@code /api/**} (spec-05).
 *
 * <ul>
 *   <li>{@link BankException} → el status que declara la excepción de dominio.</li>
 *   <li>{@link ConstraintViolationException} → 400 con {@code fieldErrors}.</li>
 *   <li>cualquier otra excepción → 500 con un mensaje genérico: ni el mensaje
 *       interno ni el stack trace se exponen en la respuesta (solo se registran
 *       en el log del servidor).</li>
 * </ul>
 *
 * <p>Todas las ramas comparten el mismo payload:
 * {@code { error, message, path, timestamp, fieldErrors? }}.
 */
@Produces
@Singleton
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<?>> {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request, Exception exception) {
        if (exception instanceof BankException bankException) {
            return response(request, bankException.status(),
                            ErrorPayload.codeOf(bankException), bankException.getMessage(), null);
        }
        if (exception instanceof ConstraintViolationException validationException) {
            return validationResponse(request, validationException);
        }

        log.error("Unhandled exception on {} {}", request.getMethod(), request.getPath(), exception);
        return response(request, HttpStatus.INTERNAL_SERVER_ERROR, ErrorPayload.INTERNAL,
                        "Unexpected internal error", null);
    }

    /** Construye la respuesta con el payload único. Visible para el handler de validación. */
    static HttpResponse<?> response(HttpRequest request, HttpStatus status, String code,
                                    String message, Map<String, String> fieldErrors) {
        return HttpResponse.status(status)
                .body(ErrorPayload.of(code, message, request.getPath(), fieldErrors));
    }

    /** Convierte cada violación en una entrada {@code campo -> mensaje}. */
    static HttpResponse<?> validationResponse(HttpRequest request,
                                              ConstraintViolationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        if (exception.getConstraintViolations() != null) {
            for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
                String field = violation.getPropertyPath() == null
                        ? "" : violation.getPropertyPath().toString();
                fieldErrors.put(field, violation.getMessage());
            }
        }
        return response(request, HttpStatus.BAD_REQUEST, ErrorPayload.VALIDATION,
                        "Validation failed", fieldErrors);
    }
}
