package io.onstructive.micronaut.exception;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.*;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.*;
import io.micronaut.http.server.exceptions.response.ErrorContext;
import io.micronaut.http.server.exceptions.response.ErrorResponseProcessor;
import jakarta.inject.Singleton;

// GlobalExceptionHandler.java
@Produces
@Singleton
@Requires(classes = { Exception.class })
public class GlobalExceptionHandler
        implements ExceptionHandler<Exception, HttpResponse<?>> {

    private static final Logger log = 
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request, Exception e) {
        // Esto imprime el stack trace completo en consola
        log.error("Unhandled exception on {} {}: {}",
            request.getMethod(), request.getPath(), e.getMessage(), e);

        return HttpResponse.serverError(
            Map.of(
                "error",   e.getClass().getSimpleName(),
                "message", e.getMessage() != null ? e.getMessage() : "null",
                "path",    request.getPath()
            )
        );
    }
}
