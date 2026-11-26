package io.onstructive.micronaut.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.security.PermitAll;
import java.util.Map;

/**
 * Sonda de vida publica (spec-06): responde {@code {"status":"UP"}} sin token.
 * El detalle (database/diskSpace) se sirve en {@code /actuator/health} via
 * micronaut-management.
 */
@Controller("/health")
@PermitAll
@Tag(name = "Health", description = "Public liveness probe (spec-06)")
public class HealthController {

    @Get
    @Operation(summary = "Liveness probe: UP while the application is running")
    public HttpResponse<Map<String, String>> health() {
        return HttpResponse.ok(Map.of("status", "UP"));
    }
}
