package io.onstructive.micronaut.exception;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Constructor del payload único de error de {@code /api/**} (spec-05):
 * {@code { error, message, path, timestamp, fieldErrors? }}.
 *
 * <p>{@code fieldErrors} solo se incluye cuando hay errores de validación,
 * para que el resto de respuestas no expongan un campo vacío.
 */
public final class ErrorPayload {

    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INSUFFICIENT_FUNDS = "INSUFFICIENT_FUNDS";
    public static final String CONFLICT = "CONFLICT";
    public static final String BUSINESS_RULE = "BUSINESS_RULE_VIOLATION";
    public static final String VALIDATION = "VALIDATION_ERROR";
    public static final String INTERNAL = "INTERNAL_ERROR";
    // spec-07: exceso de peticiones (HTTP 429) con el mismo payload único.
    public static final String RATE_LIMIT_EXCEEDED = "RATE_LIMIT_EXCEEDED";

    private ErrorPayload() {
    }

    public static Map<String, Object> of(String code, String message, String path) {
        return of(code, message, path, null);
    }

    public static Map<String, Object> of(String code, String message, String path,
                                         Map<String, String> fieldErrors) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", code);
        body.put("message", message);
        body.put("path", path);
        body.put("timestamp", Instant.now().toString());
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            body.put("fieldErrors", fieldErrors);
        }
        return body;
    }

    /** Código estable para una excepción de dominio (el status viaja en la respuesta). */
    public static String codeOf(BankException exception) {
        if (exception instanceof AccountNotFoundException) {
            return NOT_FOUND;
        }
        if (exception instanceof InsufficientFundsException) {
            return INSUFFICIENT_FUNDS;
        }
        if (exception instanceof DuplicateResourceException) {
            return CONFLICT;
        }
        if (exception instanceof BusinessRuleException) {
            return BUSINESS_RULE;
        }
        return "BANK_ERROR";
    }
}
