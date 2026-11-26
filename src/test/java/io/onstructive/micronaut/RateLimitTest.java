package io.onstructive.micronaut;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.dto.LoginRequest;
import io.onstructive.micronaut.dto.LoginResponse;
import io.onstructive.micronaut.dto.RegisterRequest;
import io.onstructive.micronaut.dto.TransactionRequest;
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.exception.ErrorPayload;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests HTTP de los tres límites de la spec-07. {@code application-test.yml}
 * desactiva el filtro por defecto; las {@code @Property} de clase lo reactivan
 * con límites pequeños. Cada test usa IPs (X-Forwarded-For) y usuarios únicos
 * para que los contadores en memoria no se contaminen entre tests.
 */
@MicronautTest(transactional = false)
@Property(name = "ratelimit.enabled", value = "true")
@Property(name = "ratelimit.per-ip.minute", value = "5")
@Property(name = "ratelimit.per-user.hour", value = "3")
@Property(name = "ratelimit.transactions.minute", value = "2")
class RateLimitTest {

    @Inject
    @Client("/")
    HttpClient client;

    private record Outcome(HttpStatus status, Map<String, Object> body) {}

    private String uniqueIp() {
        return "rl-" + UUID.randomUUID();
    }

    private String registerAndLogin(String ip) {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registration = new RegisterRequest(
                "rl-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
        client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", registration).header("X-Forwarded-For", ip),
                UserResponse.class);
        HttpResponse<LoginResponse> login = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login",
                        new LoginRequest(registration.username(), registration.password()))
                        .header("X-Forwarded-For", ip),
                LoginResponse.class);
        return login.getBody().orElseThrow().accessToken();
    }

    private Outcome send(HttpRequest<?> request) {
        try {
            return new Outcome(client.toBlocking().exchange(request, Map.class).getStatus(), null);
        } catch (HttpClientResponseException e) {
            Map<String, Object> body = e.getStatus() == HttpStatus.TOO_MANY_REQUESTS
                    ? errorBody(e) : null;
            return new Outcome(e.getStatus(), body);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> errorBody(HttpClientResponseException exception) {
        return (Map<String, Object>) exception.getResponse().getBody(Map.class).orElseThrow();
    }

    private void assertRateLimitPayload(Outcome outcome, String path) {
        assertNotNull(outcome.body(), "El 429 debe llevar el payload único: " + outcome.status());
        Map<String, Object> body = outcome.body();
        assertEquals(ErrorPayload.RATE_LIMIT_EXCEEDED, body.get("error"), body.toString());
        assertNotNull(body.get("message"), body.toString());
        assertNotNull(Instant.parse(body.get("timestamp").toString()), body.toString());
        assertEquals(path, body.get("path"), body.toString());
        assertFalse(body.containsKey("fieldErrors"), body.toString());
    }

    @Test
    void perIpMinuteLimitReturns429WithUnifiedPayload() {
        String ip = uniqueIp();
        String path = "/api/auth/login";
        Outcome last = null;
        // Sin JWT: solo aplica el contador por IP (límite 5).
        for (int i = 0; i < 6; i++) {
            last = send(HttpRequest.POST(path,
                    new LoginRequest("no-user-" + UUID.randomUUID(), "wrong password"))
                    .header("X-Forwarded-For", ip));
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, last.status());
        assertRateLimitPayload(last, path);
    }

    @Test
    void perUserHourLimitReturns429WithUnifiedPayload() {
        String token = registerAndLogin(uniqueIp()); // IP distinta: register/login no cuentan aquí
        String ip = uniqueIp();
        String path = "/api/accounts";
        Outcome last = null;
        // IP (5) no se agota; usuario (3) se excede en la cuarta petición.
        for (int i = 0; i < 4; i++) {
            last = send(HttpRequest.GET(path).bearerAuth(token).header("X-Forwarded-For", ip));
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, last.status());
        assertRateLimitPayload(last, path);
    }

    @Test
    void transactionMinuteLimitReturns429WithUnifiedPayload() {
        String token = registerAndLogin(uniqueIp());
        String ip = uniqueIp();
        String path = "/api/transactions/deposit";
        Outcome last = null;
        // IP (5) y usuario (3) no se agotan; transacciones (2) se exceden en la tercera.
        for (int i = 0; i < 3; i++) {
            last = send(HttpRequest.POST(path, new TransactionRequest(
                    UUID.randomUUID(), new BigDecimal("1.00"), "rate limit test"))
                    .bearerAuth(token).header("X-Forwarded-For", ip));
        }
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, last.status());
        assertRateLimitPayload(last, path);
    }

    @Test
    void transactionLimitCombinesDepositAndHistoryPaths() {
        String token = registerAndLogin(uniqueIp());
        String ip = uniqueIp();
        String historyPath = "/api/accounts/" + UUID.randomUUID() + "/transactions";

        // Escritura y dos consultas de historial comparten el mismo bucket del
        // límite de transacciones (2/min): la tercera petición debe ser 429.
        Outcome deposit = send(HttpRequest.POST("/api/transactions/deposit",
                new TransactionRequest(UUID.randomUUID(), new BigDecimal("1.00"), "rate limit test"))
                .bearerAuth(token).header("X-Forwarded-For", ip));
        Outcome firstHistory = send(HttpRequest.GET(historyPath)
                .bearerAuth(token).header("X-Forwarded-For", ip));
        Outcome secondHistory = send(HttpRequest.GET(historyPath)
                .bearerAuth(token).header("X-Forwarded-For", ip));

        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, deposit.status());
        assertNotEquals(HttpStatus.TOO_MANY_REQUESTS, firstHistory.status());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, secondHistory.status());
        assertRateLimitPayload(secondHistory, historyPath);
    }
}
