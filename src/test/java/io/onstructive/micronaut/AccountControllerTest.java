package io.onstructive.micronaut;

import io.micronaut.core.type.Argument;
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
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests HTTP de cuentas con JWT real (spec-03). */
@MicronautTest(transactional = false)
class AccountControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    private record TestUser(String username, String token, UUID userId) {}

    /** Registro + login reales para obtener un JWT firmado con el claim userId. */
    private TestUser registerAndLogin() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registration = new RegisterRequest(
                "acc-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
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

    private UUID jwtUserId(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                                    StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"userId\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
        assertTrue(matcher.find(), "El JWT debe contener el claim userId: " + payload);
        return UUID.fromString(matcher.group(1));
    }

    private CreateAccountRequest accountRequest(String suffix, AccountType type, BigDecimal initial) {
        return new CreateAccountRequest("ACC-" + suffix, "EUR", type, initial);
    }

    private Account createAccount(TestUser user, CreateAccountRequest request) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/accounts", request).bearerAuth(user.token()), Account.class);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return response.getBody().orElseThrow();
    }

    @Test
    void createUsesJwtUserIdAsOwnerAndDefaultBalanceIsZero() {
        TestUser user = registerAndLogin();
        String suffix = UUID.randomUUID().toString();

        Account created = createAccount(user,
                accountRequest(suffix, AccountType.SAVINGS, new BigDecimal("125.50")));

        assertEquals(jwtUserId(user.token()), created.ownerId());
        assertEquals(user.userId(), created.ownerId());
        assertEquals(AccountType.SAVINGS, created.accountType());
        assertEquals(0, new BigDecimal("125.50").compareTo(created.balance()));

        Account defaultBalance = createAccount(user,
                accountRequest("D" + suffix, AccountType.CHECKING, null));
        assertEquals(0, BigDecimal.ZERO.compareTo(defaultBalance.balance()));
        assertEquals(user.userId(), defaultBalance.ownerId());
    }

    @Test
    void listReturnsOnlyAccountsOwnedByTheAuthenticatedUser() {
        TestUser owner = registerAndLogin();
        TestUser other = registerAndLogin();
        String suffix = UUID.randomUUID().toString();
        Account mine = createAccount(owner, accountRequest("M" + suffix, AccountType.CHECKING, null));
        Account foreign = createAccount(other, accountRequest("F" + suffix, AccountType.SAVINGS, null));

        HttpResponse<List<Account>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts").bearerAuth(owner.token()),
                Argument.listOf(Account.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        List<Account> body = response.getBody().orElseThrow();
        assertEquals(1, body.size());
        assertEquals(mine.id(), body.getFirst().id());
        assertEquals(jwtUserId(owner.token()), body.getFirst().ownerId());
        assertFalse(body.stream().anyMatch(a -> a.id().equals(foreign.id())));
    }

    @Test
    void getOfForeignOrMissingAccountReturns404() {
        TestUser owner = registerAndLogin();
        TestUser other = registerAndLogin();
        Account mine = createAccount(owner,
                accountRequest(UUID.randomUUID().toString(), AccountType.CHECKING, null));

        HttpClientResponseException foreignError = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + mine.id()).bearerAuth(other.token()),
                        Account.class));
        assertEquals(HttpStatus.NOT_FOUND, foreignError.getStatus());

        HttpClientResponseException missingError = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + UUID.randomUUID()).bearerAuth(owner.token()),
                        Account.class));
        assertEquals(HttpStatus.NOT_FOUND, missingError.getStatus());
    }

    @Test
    void deleteWithBalanceIs4xxAndOwnedZeroBalanceDeleteWorks() {
        TestUser owner = registerAndLogin();
        TestUser other = registerAndLogin();
        String suffix = UUID.randomUUID().toString();

        Account withBalance = createAccount(owner,
                accountRequest("B" + suffix, AccountType.CHECKING, new BigDecimal("10.00")));
        HttpClientResponseException balanceError = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.DELETE("/api/accounts/" + withBalance.id()).bearerAuth(owner.token()),
                        String.class));
        assertTrue(balanceError.getStatus().getCode() >= 400
                && balanceError.getStatus().getCode() < 500,
                "Borrar con saldo debe dar 4xx, fue " + balanceError.getStatus());

        Account empty = createAccount(owner, accountRequest("Z" + suffix, AccountType.CHECKING, null));
        HttpClientResponseException foreignDelete = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.DELETE("/api/accounts/" + empty.id()).bearerAuth(other.token()),
                        String.class));
        assertEquals(HttpStatus.NOT_FOUND, foreignDelete.getStatus());

        HttpResponse<String> deleted = client.toBlocking().exchange(
                HttpRequest.DELETE("/api/accounts/" + empty.id()).bearerAuth(owner.token()),
                String.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatus());

        HttpClientResponseException gone = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + empty.id()).bearerAuth(owner.token()),
                        Account.class));
        assertEquals(HttpStatus.NOT_FOUND, gone.getStatus());
    }

    @Test
    void accessWithoutTokenReturns401() {
        HttpClientResponseException listError = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/api/accounts"), List.class));
        assertEquals(HttpStatus.UNAUTHORIZED, listError.getStatus());

        HttpClientResponseException createError = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/accounts",
                                accountRequest("N" + UUID.randomUUID(), AccountType.CHECKING, null)),
                        Account.class));
        assertEquals(HttpStatus.UNAUTHORIZED, createError.getStatus());
    }

    @Test
    void legacyAccountsRouteIsGone() {
        TestUser user = registerAndLogin();
        HttpClientResponseException error = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/accounts").bearerAuth(user.token()), List.class));
        // Micronaut Security responde 403 a una ruta sin regla de autorización;
        // lo importante es que el legacy /accounts ya no devuelve un recurso.
        assertTrue(error.getStatus().getCode() >= 400 && error.getStatus().getCode() < 500,
                "La ruta legacy /accounts debe estar fuera de servicio, fue " + error.getStatus());
    }
}
