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
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountType;
import io.onstructive.micronaut.model.TransactionStatus;
import io.onstructive.micronaut.model.TransactionType;
import io.onstructive.micronaut.repository.TransactionRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests HTTP de transacciones con JWT real (spec-04). */
@MicronautTest(transactional = false)
class TransactionControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TransactionRepository transactionRepository;

    private record TestUser(String username, String token, UUID userId) {}

    /** Registro + login reales para obtener un JWT firmado con el claim userId. */
    private TestUser registerAndLogin() {
        String suffix = UUID.randomUUID().toString();
        RegisterRequest registration = new RegisterRequest(
                "tx-" + suffix, suffix + "@example.com", "Str0ng-Passw0rd!");
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

    private Account createAccount(TestUser user, String suffix, String initialBalance) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.POST("/api/accounts", new CreateAccountRequest(
                        "ACC-" + suffix, "EUR", AccountType.CHECKING,
                        new BigDecimal(initialBalance))).bearerAuth(user.token()),
                Account.class);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return response.getBody().orElseThrow();
    }

    /** Saldo leído por HTTP (conexión distinta): comprueba datos confirmados. */
    private BigDecimal balanceOf(TestUser user, UUID accountId) {
        HttpResponse<Account> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts/" + accountId).bearerAuth(user.token()),
                Account.class);
        return response.getBody().orElseThrow().balance();
    }

    private HttpResponse<TransactionResponse> post(String path, Object body, String token) {
        return client.toBlocking().exchange(HttpRequest.POST(path, body).bearerAuth(token),
                                            TransactionResponse.class);
    }

    private List<TransactionResponse> history(TestUser user, UUID accountId) {
        HttpResponse<List<TransactionResponse>> response = client.toBlocking().exchange(
                HttpRequest.GET("/api/accounts/" + accountId + "/transactions")
                        .bearerAuth(user.token()),
                Argument.listOf(TransactionResponse.class));
        assertEquals(HttpStatus.OK, response.getStatus());
        return response.getBody().orElseThrow();
    }

    private void assert4xx(HttpClientResponseException error) {
        assertTrue(error.getStatus().getCode() >= 400 && error.getStatus().getCode() < 500,
                   "Se esperaba 4xx, fue " + error.getStatus());
    }

    @Test
    void depositUpdatesBalanceAndCreatesExactlyOneTransaction() {
        TestUser user = registerAndLogin();
        Account account = createAccount(user, "D" + UUID.randomUUID(), "100.00");
        long before = transactionRepository.count();

        HttpResponse<TransactionResponse> response = post("/api/transactions/deposit",
                new TransactionRequest(account.id(), new BigDecimal("25.50"), "salary"),
                user.token());

        assertEquals(HttpStatus.CREATED, response.getStatus());
        TransactionResponse body = response.getBody().orElseThrow();
        assertNotNull(body.id());
        assertNotNull(body.timestamp());
        assertEquals(TransactionType.DEPOSIT, body.type());
        assertEquals(TransactionStatus.COMPLETED, body.status());
        assertEquals(0, new BigDecimal("25.50").compareTo(body.amount()));
        assertNull(body.fromAccount());
        assertEquals(account.id(), body.toAccount());
        assertEquals("salary", body.description());

        assertEquals(0, new BigDecimal("125.50").compareTo(balanceOf(user, account.id())));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void withdrawUpdatesBalanceAndCreatesExactlyOneTransaction() {
        TestUser user = registerAndLogin();
        Account account = createAccount(user, "W" + UUID.randomUUID(), "100.00");
        long before = transactionRepository.count();

        // description omitida: el campo es opcional (spec-04).
        HttpResponse<TransactionResponse> response = post("/api/transactions/withdraw",
                new TransactionRequest(account.id(), new BigDecimal("40.00"), null),
                user.token());

        assertEquals(HttpStatus.CREATED, response.getStatus());
        TransactionResponse body = response.getBody().orElseThrow();
        assertEquals(TransactionType.WITHDRAWAL, body.type());
        assertEquals(TransactionStatus.COMPLETED, body.status());
        assertEquals(account.id(), body.fromAccount());
        assertNull(body.toAccount());
        assertNull(body.description());

        assertEquals(0, new BigDecimal("60.00").compareTo(balanceOf(user, account.id())));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void transferUpdatesBothBalancesAndCreatesExactlyOneTransaction() {
        TestUser user = registerAndLogin();
        Account from = createAccount(user, "F" + UUID.randomUUID(), "100.00");
        Account to = createAccount(user, "T" + UUID.randomUUID(), "50.00");
        long before = transactionRepository.count();

        HttpResponse<TransactionResponse> response = post("/api/transactions/transfer",
                new TransferRequest(from.id(), to.id(), new BigDecimal("30.00"), "rent"),
                user.token());

        assertEquals(HttpStatus.CREATED, response.getStatus());
        TransactionResponse body = response.getBody().orElseThrow();
        assertEquals(TransactionType.TRANSFER, body.type());
        assertEquals(TransactionStatus.COMPLETED, body.status());
        assertEquals(from.id(), body.fromAccount());
        assertEquals(to.id(), body.toAccount());
        assertEquals("rent", body.description());

        assertEquals(0, new BigDecimal("70.00").compareTo(balanceOf(user, from.id())));
        assertEquals(0, new BigDecimal("80.00").compareTo(balanceOf(user, to.id())));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void historyReturnsMovementsInBothDirections() {
        TestUser user = registerAndLogin();
        Account a = createAccount(user, "HA" + UUID.randomUUID(), "100.00");
        Account b = createAccount(user, "HB" + UUID.randomUUID(), "0.00");
        long before = transactionRepository.count();

        post("/api/transactions/deposit",
             new TransactionRequest(a.id(), new BigDecimal("20.00"), null), user.token());
        post("/api/transactions/transfer",
             new TransferRequest(a.id(), b.id(), new BigDecimal("30.00"), null), user.token());

        List<TransactionResponse> historyA = history(user, a.id());
        assertEquals(2, historyA.size(), "El historial de A incluye depósito (destino) y transferencia (origen)");
        assertTrue(historyA.stream().anyMatch(t -> t.type() == TransactionType.DEPOSIT
                                                   && a.id().equals(t.toAccount())));
        assertTrue(historyA.stream().anyMatch(t -> t.type() == TransactionType.TRANSFER
                                                   && a.id().equals(t.fromAccount())));

        List<TransactionResponse> historyB = history(user, b.id());
        assertEquals(1, historyB.size());
        assertEquals(TransactionType.TRANSFER, historyB.getFirst().type());
        assertEquals(b.id(), historyB.getFirst().toAccount());

        assertEquals(before + 2, transactionRepository.count());
    }

    @Test
    void insufficientFundsRollsBackBalancesAndLeavesNoTransaction() {
        TestUser user = registerAndLogin();
        Account a = createAccount(user, "IA" + UUID.randomUUID(), "10.00");
        Account b = createAccount(user, "IB" + UUID.randomUUID(), "0.00");
        long before = transactionRepository.count();

        HttpClientResponseException transferError = assertThrows(HttpClientResponseException.class,
                () -> post("/api/transactions/transfer",
                           new TransferRequest(a.id(), b.id(), new BigDecimal("100.00"), "fail"),
                           user.token()));
        assert4xx(transferError);

        HttpClientResponseException withdrawError = assertThrows(HttpClientResponseException.class,
                () -> post("/api/transactions/withdraw",
                           new TransactionRequest(a.id(), new BigDecimal("50.00"), "fail"),
                           user.token()));
        assert4xx(withdrawError);

        assertEquals(0, new BigDecimal("10.00").compareTo(balanceOf(user, a.id())));
        assertEquals(0, new BigDecimal("0.00").compareTo(balanceOf(user, b.id())));
        assertEquals(before, transactionRepository.count(),
                     "El rollback no debe dejar filas de Transaction");
    }

    @Test
    void foreignAccountReturns404AndDoesNotChangeAnything() {
        TestUser owner = registerAndLogin();
        TestUser other = registerAndLogin();
        Account mine = createAccount(owner, "OM" + UUID.randomUUID(), "100.00");
        Account foreign = createAccount(other, "OX" + UUID.randomUUID(), "20.00");
        long before = transactionRepository.count();

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(HttpClientResponseException.class,
                () -> post("/api/transactions/deposit",
                           new TransactionRequest(foreign.id(), new BigDecimal("5.00"), null),
                           owner.token())).getStatus());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(HttpClientResponseException.class,
                () -> post("/api/transactions/withdraw",
                           new TransactionRequest(foreign.id(), new BigDecimal("5.00"), null),
                           owner.token())).getStatus());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(HttpClientResponseException.class,
                () -> post("/api/transactions/transfer",
                           new TransferRequest(mine.id(), foreign.id(), new BigDecimal("5.00"), null),
                           owner.token())).getStatus());

        assertEquals(HttpStatus.NOT_FOUND, assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + foreign.id() + "/transactions")
                                .bearerAuth(owner.token()),
                        Argument.listOf(TransactionResponse.class))).getStatus());

        assertEquals(0, new BigDecimal("100.00").compareTo(balanceOf(owner, mine.id())));
        assertEquals(0, new BigDecimal("20.00").compareTo(balanceOf(other, foreign.id())));
        assertEquals(before, transactionRepository.count());
    }

    @Test
    void accessWithoutTokenReturns401() {
        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/transactions/deposit",
                                new TransactionRequest(UUID.randomUUID(), BigDecimal.ONE, null)),
                        TransactionResponse.class)).getStatus());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/transactions/withdraw",
                                new TransactionRequest(UUID.randomUUID(), BigDecimal.ONE, null)),
                        TransactionResponse.class)).getStatus());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.POST("/api/transactions/transfer",
                                new TransferRequest(UUID.randomUUID(), UUID.randomUUID(),
                                                    BigDecimal.ONE, null)),
                        TransactionResponse.class)).getStatus());

        assertEquals(HttpStatus.UNAUTHORIZED, assertThrows(HttpClientResponseException.class,
                () -> client.toBlocking().exchange(
                        HttpRequest.GET("/api/accounts/" + UUID.randomUUID() + "/transactions"),
                        Argument.listOf(TransactionResponse.class))).getStatus());
    }
}
