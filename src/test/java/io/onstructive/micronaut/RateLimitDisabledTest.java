package io.onstructive.micronaut;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.dto.LoginRequest;
import jakarta.inject.Inject;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Comprueba que {@code ratelimit.enabled=false} desactiva por completo el
 * filtro: con límites de 1/2 peticiones, 10 peticiones no producen ningún 429.
 * El desactivado también viene de {@code application-test.yml}; la @Property
 * lo explicita para que la prueba sea autocontenida.
 */
@MicronautTest(transactional = false)
@Property(name = "ratelimit.enabled", value = "false")
@Property(name = "ratelimit.per-ip.minute", value = "2")
@Property(name = "ratelimit.per-user.hour", value = "1")
@Property(name = "ratelimit.transactions.minute", value = "1")
class RateLimitDisabledTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void disabledFilterNeverReturns429() {
        String ip = "rl-disabled-" + UUID.randomUUID();
        for (int i = 0; i < 10; i++) {
            HttpStatus status;
            try {
                status = client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/login",
                                new LoginRequest("no-user-" + UUID.randomUUID(), "wrong password"))
                                .header("X-Forwarded-For", ip),
                        Map.class).getStatus();
            } catch (HttpClientResponseException e) {
                status = e.getStatus();
            }
            assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, status,
                    "Con ratelimit.enabled=false no debe haber 429");
        }
    }
}
