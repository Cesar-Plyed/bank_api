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
import io.onstructive.micronaut.dto.TransactionRequest;
import io.onstructive.micronaut.dto.TransactionResponse;
import io.onstructive.micronaut.dto.TransferRequest;
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.exception.ErrorPayload;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountType;
import io.onstructive.micronaut.model.TransactionStatus;
import io.onstructive.micronaut.model.TransactionType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Flujo extremo a extremo sobre el servidor HTTP real: registro, login, cuentas,
 * transacciones, historial y borrado con saldo 0 (spec-08).
 */
@MicronautTest(transactional = false)
class BankApiIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    private record TestUser(String username, String password, String token, UUID userId) {}

    private TestUser registerAndLogin() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registration = new RegisterRequest(
                "e2e-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
        HttpResponse<UserResponse> registered = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/register", registration), UserResponse.class);
        assertEquals(HttpStatus.CREATED, registered.getStatus());

        HttpResponse<LoginResponse> loggedIn = client.toBlocking().exchange(
                HttpRequest.POST("/api/auth/login",
                        new LoginRequest(registration.username(), registration.password())),
                LoginResponse.class);
        assertEquals(HttpStatus.OK, loggedIn.getStatus());
        LoginResponse login = loggedIn.getBody().orElseThrow();
        assertEquals("Bearer", login.tokenType());
        assertEquals(3600, login.expiresIn());
        assertEquals(3, login.accessToken().split("\\.", -1).length);
        return new TestUser(registration.username(), registration.password(),
                            login.accessToken(), registered.getBody().orElseThrow().id());
    }

    private Account createAccount(TestUser user, String suffix, String initialBalance) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/accounts", new CreateAccountRequest(
                        "E2E-" + suffix, "EUR", AccountType.CHECKING,
                        new BigDecimal(initialBalance))).bearerAuth(user.token()),
                Account.class);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        Account account = response.getBody().orElseThrow();
        assertEquals(user.userId(), account.ownerId());
        return account;
    }

    private BigDecimal balanceOf(TestUser user, UUID accountId) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts/" + accountId).bearerAuth(user.token()),
                Account.class);
        assertEquals(HttpStatus.OK, response.getStatus());
        return response.getBody().orElseThrow().balance();
    }

    private HttpResponse<TransactionResponse> transaction(String path, Object body, TestUser user) {
        return client.toBlocking().exchange(
                HttpRequest.POST(path, body).bearerAuth(user.token()), TransactionResponse.class);
    }

    private List<TransactionResponse> historyOf(TestUser user, UUID accountId) {
        HttpResponse<List<TransactionResponse>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts/" + accountId + "/transactions")
                        .bearerAuth(user.token()),
                Argument.listOf(TransactionResponse.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        return response.getBody().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> errorBody(HttpClientResponseException exception) {
        return (Map<String, Object>) exception.getResponse().getBody(Map.class).orElseThrow();
    }

    private void assertErrorPayload(HttpClientResponseException exception, HttpStatus status,
                                    String code, String path) {
        assertEquals(status, exception.getStatus());
        Map<String, Object> body = errorBody(exception);
        assertEquals(code, body.get("error"), body.toString());
        assertNotNull(body.get("message"), body.toString());
        assertEquals(path, body.get("path"), body.toString());
        assertNotNull(Instant.parse(body.get("timestamp").toString()));
        assertFalse(body.toString().contains("stackTrace"), body.toString());
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }

    @Test
    void completeFlowFromRegistrationToAccountDeletion() {
        TestUser user = registerAndLogin();
        String suffix = UUID.randomUUID().toString();

        // Dos cuentas propias: la segunda arranca vacía.
        Account source = createAccount(user, "S" + suffix, "100.00");
        Account target = createAccount(user, "T" + suffix, "0.00");

        HttpResponse<List<Account>> listed = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts").bearerAuth(user.token()),
                Argument.listOf(Account.class));
        assertEquals(HttpStatus.OK, listed.getStatus());
        assertEquals(2, listed.getBody().orElseThrow().size());

        // Depósito.
        HttpResponse<TransactionResponse> deposited = transaction("/api/transactions/deposit",
                new TransactionRequest(source.id(), new BigDecimal("25.50"), "salary"), user);
        assertEquals(HttpStatus.CREATED, deposited.getStatus());
        TransactionResponse deposit = deposited.getBody().orElseThrow();
        assertEquals(TransactionType.DEPOSIT, deposit.type());
        assertEquals(TransactionStatus.COMPLETED, deposit.status());
        assertNull(deposit.fromAccount());
        assertEquals(source.id(), deposit.toAccount());
        assertMoney("125.50", balanceOf(user, source.id()));

        // Retiro.
        HttpResponse<TransactionResponse> withdrawn = transaction("/api/transactions/withdraw",
                new TransactionRequest(source.id(), new BigDecimal("15.50"), "groceries"), user);
        assertEquals(HttpStatus.CREATED, withdrawn.getStatus());
        TransactionResponse withdrawal = withdrawn.getBody().orElseThrow();
        assertEquals(TransactionType.WITHDRAWAL, withdrawal.type());
        assertEquals(TransactionStatus.COMPLETED, withdrawal.status());
        assertEquals(source.id(), withdrawal.fromAccount());
        assertNull(withdrawal.toAccount());
        assertMoney("110.00", balanceOf(user, source.id()));

        // Transferencia entre las dos cuentas propias.
        HttpResponse<TransactionResponse> transferred = transaction("/api/transactions/transfer",
                new TransferRequest(source.id(), target.id(), new BigDecimal("30.00"), "rent"), user);
        assertEquals(HttpStatus.CREATED, transferred.getStatus());
        TransactionResponse transfer = transferred.getBody().orElseThrow();
        assertEquals(TransactionType.TRANSFER, transfer.type());
        assertEquals(TransactionStatus.COMPLETED, transfer.status());
        assertEquals(source.id(), transfer.fromAccount());
        assertEquals(target.id(), transfer.toAccount());
        assertNotNull(transfer.timestamp());
        assertMoney("80.00", balanceOf(user, source.id()));
        assertMoney("30.00", balanceOf(user, target.id()));

        // Historial en ambos sentidos.
        List<TransactionResponse> sourceHistory = historyOf(user, source.id());
        assertEquals(3, sourceHistory.size());
        assertTrue(sourceHistory.stream().anyMatch(tx -> tx.type() == TransactionType.DEPOSIT
                                                        && source.id().equals(tx.toAccount())));
        assertTrue(sourceHistory.stream().anyMatch(tx -> tx.type() == TransactionType.WITHDRAWAL
                                                        && source.id().equals(tx.fromAccount())));
        assertTrue(sourceHistory.stream().anyMatch(tx -> tx.type() == TransactionType.TRANSFER
                                                        && source.id().equals(tx.fromAccount())));
        List<TransactionResponse> targetHistory = historyOf(user, target.id());
        assertEquals(1, targetHistory.size());
        assertEquals(TransactionType.TRANSFER, targetHistory.getFirst().type());
        assertEquals(target.id(), targetHistory.getFirst().toAccount());

        // Borrar con saldo se rechaza; vaciar la cuenta y borrarla devuelve 204.
        HttpClientResponseException withBalance = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.DELETE("/api/accounts/" + target.id()).bearerAuth(user.token()),
                        String.class));
        assertErrorPayload(withBalance, HttpStatus.BAD_REQUEST,
                           ErrorPayload.BUSINESS_RULE, "/api/accounts/" + target.id());

        HttpResponse<TransactionResponse> emptied = transaction("/api/transactions/withdraw",
                new TransactionRequest(target.id(), new BigDecimal("30.00"), "close"), user);
        assertEquals(HttpStatus.CREATED, emptied.getStatus());
        assertMoney("0.00", balanceOf(user, target.id()));

        HttpResponse<Void> deleted = client.toBlocking().exchange(
                HttpRequest.DELETE("/api/accounts/" + target.id()).bearerAuth(user.token()),
                Void.class);
        assertEquals(HttpStatus.NO_CONTENT, deleted.getStatus());

        HttpClientResponseException gone = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + target.id()).bearerAuth(user.token()),
                        Account.class));
        assertErrorPayload(gone, HttpStatus.NOT_FOUND, ErrorPayload.NOT_FOUND,
                           "/api/accounts/" + target.id());
    }

    @Test
    void errorPayloadsStayConsistentAcrossTheWholeApi() {
        TestUser user = registerAndLogin();
        String suffix = UUID.randomUUID().toString();
        Account account = createAccount(user, "E" + suffix, "10.00");

        // 409 en registro duplicado y en número de cuenta duplicado.
        RegisterRequest duplicateUser = new RegisterRequest(
                user.username(), UUID.randomUUID() + "@example.com", "Another-Passw0rd!");
        HttpClientResponseException duplicateRegistration = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/register", duplicateUser), UserResponse.class));
        assertErrorPayload(duplicateRegistration, HttpStatus.CONFLICT,
                           ErrorPayload.CONFLICT, "/api/auth/register");

        HttpClientResponseException duplicateAccount = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/accounts", new CreateAccountRequest(
                                account.accountNumber(), "EUR", AccountType.SAVINGS, BigDecimal.ZERO))
                                .bearerAuth(user.token()),
                        Account.class));
        assertErrorPayload(duplicateAccount, HttpStatus.CONFLICT,
                           ErrorPayload.CONFLICT, "/api/accounts");

        // 401 en login con credenciales inválidas y sin token en ruta protegida.
        HttpClientResponseException badCredentials = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/auth/login",
                                new LoginRequest(user.username(), "wrong-password")),
                        LoginResponse.class));
        assertErrorPayload(badCredentials, HttpStatus.UNAUTHORIZED,
                           "AUTHENTICATION_FAILED", "/api/auth/login");
        HttpClientResponseException noToken = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(HttpRequest.GET("/api/accounts"), List.class));
        assertEquals(HttpStatus.UNAUTHORIZED, noToken.getStatus());

        // 404 en cuenta ajena (GET y depósito).
        TestUser other = registerAndLogin();
        Account foreign = createAccount(other, "X" + suffix, "20.00");
        HttpClientResponseException foreignGet = assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + foreign.id()).bearerAuth(user.token()),
                        Account.class));
        assertErrorPayload(foreignGet, HttpStatus.NOT_FOUND, ErrorPayload.NOT_FOUND,
                           "/api/accounts/" + foreign.id());
        HttpClientResponseException foreignDeposit = assertThrows(
                HttpClientResponseException.class,
                () -> transaction("/api/transactions/deposit",
                        new TransactionRequest(foreign.id(), BigDecimal.ONE, null), user));
        assertErrorPayload(foreignDeposit, HttpStatus.NOT_FOUND, ErrorPayload.NOT_FOUND,
                           "/api/transactions/deposit");

        // 400 con códigos estables: fondos insuficientes y regla de negocio.
        HttpClientResponseException insufficient = assertThrows(
                HttpClientResponseException.class,
                () -> transaction("/api/transactions/withdraw",
                        new TransactionRequest(account.id(), new BigDecimal("100.00"), null), user));
        assertErrorPayload(insufficient, HttpStatus.BAD_REQUEST,
                           ErrorPayload.INSUFFICIENT_FUNDS, "/api/transactions/withdraw");

        HttpClientResponseException sameAccount = assertThrows(
                HttpClientResponseException.class,
                () -> transaction("/api/transactions/transfer",
                        new TransferRequest(account.id(), account.id(), BigDecimal.ONE, null), user));
        assertErrorPayload(sameAccount, HttpStatus.BAD_REQUEST,
                           ErrorPayload.BUSINESS_RULE, "/api/transactions/transfer");

        HttpClientResponseException deleteWithBalance = assertThrows(
                HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.DELETE("/api/accounts/" + account.id()).bearerAuth(user.token()),
                        String.class));
        assertErrorPayload(deleteWithBalance, HttpStatus.BAD_REQUEST,
                           ErrorPayload.BUSINESS_RULE, "/api/accounts/" + account.id());

        // 400 de validación con fieldErrors.
        HttpClientResponseException invalid = assertThrows(
                HttpClientResponseException.class,
                () -> transaction("/api/transactions/deposit",
                        new TransactionRequest(null, new BigDecimal("-1.00"), null), user));
        assertErrorPayload(invalid, HttpStatus.BAD_REQUEST,
                           ErrorPayload.VALIDATION, "/api/transactions/deposit");
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldErrors = (Map<String, Object>) errorBody(invalid).get("fieldErrors");
        assertNotNull(fieldErrors);
        assertTrue(fieldErrors.keySet().toString().contains("accountId"));
        assertTrue(fieldErrors.keySet().toString().contains("amount"));
    }
}
