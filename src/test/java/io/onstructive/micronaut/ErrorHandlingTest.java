package io.onstructive.micronaut;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.dto.CreateAccountRequest;
import io.onstructive.micronaut.dto.LoginRequest;
import io.onstructive.micronaut.dto.LoginResponse;
import io.onstructive.micronaut.dto.RegisterRequest;
import io.onstructive.micronaut.dto.TransactionRequest;
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.exception.ErrorPayload;
import io.onstructive.micronaut.exception.GlobalExceptionHandler;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de errores con el payload único de la spec-05. */
@MicronautTest(transactional = false)
class ErrorHandlingTest {

    @Inject
    @Client("/")
    HttpClient client;

    private record TestUser(String username, String token, UUID userId) {}

    private TestUser registerAndLogin() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registration = new RegisterRequest(
                "err-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
        HttpResponse<UserResponse> registered = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", registration), UserResponse.class);
        HttpResponse<LoginResponse> loggedIn = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login",
                        new LoginRequest(registration.username(), registration.password())),
                LoginResponse.class);
        return new TestUser(registration.username(),
                loggedIn.getBody().orElseThrow().accessToken(),
                registered.getBody().orElseThrow().id());
    }

    private Account createAccount(TestUser user, String accountNumber, String balance) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/accounts", new CreateAccountRequest(
                        accountNumber, "EUR", AccountType.CHECKING, new BigDecimal(balance)))
                        .bearerAuth(user.token()),
                Account.class);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return response.getBody().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> errorBody(HttpClientResponseException exception) {
        return (Map<String, Object>) exception.getResponse().getBody(Map.class).orElseThrow();
    }

    private void assertCommonPayload(Map<String, Object> body, String code, String path) {
        assertEquals(code, body.get("error"), body.toString());
        assertNotNull(body.get("message"), body.toString());
        assertNotNull(Instant.parse(body.get("timestamp").toString()), body.toString());
        assertEquals(path, body.get("path"), body.toString());
        assertFalse(body.toString().contains("stackTrace"), body.toString());
        assertFalse(body.toString().contains("RuntimeException"), body.toString());
    }

    @Test
    void notFoundUsesUnifiedPayload() {
        TestUser user = registerAndLogin();
        UUID missing = UUID.randomUUID();
        String path = "/api/accounts/" + missing;

        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET(path).bearerAuth(user.token()), Account.class));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.NOT_FOUND, path);
        assertTrue(body.get("message").toString().contains(missing.toString()));
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void insufficientFundsUsesUnifiedPayload() {
        TestUser user = registerAndLogin();
        Account account = createAccount(user, "ACC-IF-" + UUID.randomUUID(), "0.00");
        String path = "/api/transactions/withdraw";

        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST(path, new TransactionRequest(
                                account.id(), new BigDecimal("10.00"), "fail"))
                                .bearerAuth(user.token()),
                        Map.class));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.INSUFFICIENT_FUNDS, path);
        assertEquals("Insufficient funds", body.get("message"));
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void duplicateAccountNumberUsesUnifiedPayload() {
        TestUser user = registerAndLogin();
        String number = "ACC-DUP-" + UUID.randomUUID();
        createAccount(user, number, "10.00");
        String path = "/api/accounts";

        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST(path, new CreateAccountRequest(
                                number, "EUR", AccountType.SAVINGS, BigDecimal.ZERO))
                                .bearerAuth(user.token()),
                        Account.class));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.CONFLICT, path);
        assertTrue(body.get("message").toString().contains(number));
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void invalidBodyUsesUnifiedPayloadWithFieldErrors() {
        TestUser user = registerAndLogin();
        String path = "/api/transactions/deposit";

        // accountId nulo (+  @NotNull) y amount negativo (@Positive): dos violaciones.
        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST(path, new TransactionRequest(
                                null, new BigDecimal("-1.00"), null))
                                .bearerAuth(user.token()),
                        Map.class));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.VALIDATION, path);
        assertTrue(body.containsKey("fieldErrors"), body.toString());

        @SuppressWarnings("unchecked")
        Map<String, Object> fieldErrors = (Map<String, Object>) body.get("fieldErrors");
        assertNotNull(fieldErrors);
        assertFalse(fieldErrors.isEmpty());
        String fields = fieldErrors.keySet().toString();
        assertTrue(fields.contains("accountId"), fields);
        assertTrue(fields.contains("amount"), fields);
    }

    @Test
    void businessRuleUsesUnifiedPayload() {
        TestUser user = registerAndLogin();
        Account account = createAccount(user, "ACC-BR-" + UUID.randomUUID(), "5.00");

        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.DELETE("/api/accounts/" + account.id()).bearerAuth(user.token()),
                        Map.class));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.BUSINESS_RULE, "/api/accounts/" + account.id());
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void duplicateRegistrationUsesUnifiedPayload() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest first = new RegisterRequest(
                "authdup-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
        client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", first), UserResponse.class);

        RegisterRequest duplicate = new RegisterRequest(
                first.username(), UUID.randomUUID() + "@example.com", "Another-Passw0rd!");
        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/register", duplicate), UserResponse.class));

        assertEquals(HttpStatus.CONFLICT, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, ErrorPayload.CONFLICT, "/api/auth/register");
        assertTrue(body.get("message").toString().contains("already exists"), body.toString());
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void invalidCredentialsUseUnifiedPayload() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registered = new RegisterRequest(
                "authcred-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
        client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", registered), UserResponse.class);

        HttpClientResponseException error = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/login",
                                new LoginRequest(registered.username(), "wrong-password")),
                        LoginResponse.class));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
        Map<String, Object> body = errorBody(error);
        assertCommonPayload(body, "AUTHENTICATION_FAILED", "/api/auth/login");
        assertEquals("invalid credentials", body.get("message"));
        assertFalse(body.containsKey("fieldErrors"));
    }

    @Test
    void unexpectedErrorReturns500WithoutStackTraceOrInternalMessage() {
        HttpResponse<?> response = new GlobalExceptionHandler().handle(
                HttpRequest.GET("/api/accounts"),
                new RuntimeException("TOP_SECRET_INTERNAL_DETAIL"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody().orElseThrow();
        assertEquals(ErrorPayload.INTERNAL, body.get("error"));
        assertEquals("Unexpected internal error", body.get("message"));
        assertNotNull(Instant.parse(body.get("timestamp").toString()));
        assertEquals("/api/accounts", body.get("path"));
        assertFalse(body.toString().contains("TOP_SECRET_INTERNAL_DETAIL"), body.toString());
        assertFalse(body.toString().contains("stackTrace"), body.toString());
        assertFalse(body.containsKey("fieldErrors"));
    }
}
