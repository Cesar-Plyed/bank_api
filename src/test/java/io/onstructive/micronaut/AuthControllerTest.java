package io.onstructive.micronaut;

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
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.model.User;
import io.onstructive.micronaut.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests HTTP de registro/login JWT (spec-02). */
@MicronautTest(transactional = false)
class AuthControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    UserRepository userRepository;

    private RegisterRequest randomUser() {
        String suffix = UUID.randomUUID().toString();
        return new RegisterRequest(
                "auth-" + suffix,
                suffix + "@example.com",
                "Str0ng-Passw0rd!");
    }

    @Test
    void registerCreatesUserWithBcryptHash() {
        RegisterRequest request = randomUser();

        HttpResponse<UserResponse> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", request), UserResponse.class);

        assertEquals(HttpStatus.CREATED, response.getStatus());
        UserResponse body = response.getBody().orElseThrow();
        assertEquals(request.username(), body.username());
        assertEquals(request.email(), body.email());
        assertNotNull(body.id());

        User stored = userRepository.findByUsername(request.username()).orElseThrow();
        assertTrue(stored.passwordHash().startsWith("$2a$")
                || stored.passwordHash().startsWith("$2b$"));
        assertFalse(stored.passwordHash().contains(request.password()));
    }

    @Test
    void registerRejectsDuplicateUsername() {
        RegisterRequest first = randomUser();
        client.toBlocking().exchange(HttpRequest.POST("/api/auth/register", first), UserResponse.class);

        RegisterRequest duplicate = new RegisterRequest(
                first.username(), UUID.randomUUID() + "@example.com", "Another-Passw0rd!");
        HttpClientResponseException error = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/register", duplicate), UserResponse.class));

        assertTrue(error.getStatus().getCode() >= 400 && error.getStatus().getCode() < 500,
                "El registro duplicado debe devolver 4xx, fue " + error.getStatus());
    }

    @Test
    void loginReturnsSignedJwt() {
        RegisterRequest registered = randomUser();
        client.toBlocking().exchange(HttpRequest.POST("/api/auth/register", registered), UserResponse.class);

        HttpResponse<LoginResponse> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login",
                        new LoginRequest(registered.username(), registered.password())),
                LoginResponse.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        LoginResponse body = response.getBody().orElseThrow();
        assertEquals("Bearer", body.tokenType());
        assertEquals(3600, body.expiresIn());
        assertEquals(registered.username(), body.user().username());
        assertEquals(registered.email(), body.user().email());

        String[] segments = body.accessToken().split("\\.", -1);
        assertEquals(3, segments.length, "El accessToken debe ser un JWT de 3 segmentos");
        assertTrue(segments[0].length() > 0 && segments[1].length() > 0 && segments[2].length() > 0);

        String payload = new String(java.util.Base64.getUrlDecoder().decode(segments[1]));
        assertTrue(payload.contains("\"sub\":\"" + registered.username() + "\""),
                "El subject del JWT debe ser el username: " + payload);
        assertTrue(payload.contains("\"exp\":") && payload.contains("\"iat\":"), payload);
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {
        RegisterRequest registered = randomUser();
        client.toBlocking().exchange(HttpRequest.POST("/api/auth/register", registered), UserResponse.class);

        HttpClientResponseException error = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/login",
                                new LoginRequest(registered.username(), "wrong-password")),
                        LoginResponse.class));

        assertEquals(HttpStatus.UNAUTHORIZED, error.getStatus());
    }
}
