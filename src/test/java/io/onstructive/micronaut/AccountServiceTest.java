package io.onstructive.micronaut;

import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.dto.CreateAccountRequest;
import io.onstructive.micronaut.exception.AccountNotFoundException;
import io.onstructive.micronaut.exception.BusinessRuleException;
import io.onstructive.micronaut.exception.DuplicateResourceException;
import io.onstructive.micronaut.exception.InsufficientFundsException;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountStatus;
import io.onstructive.micronaut.model.AccountType;
import io.onstructive.micronaut.repository.AccountRepository;
import io.onstructive.micronaut.service.AccountService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de servicio de cuentas contra la BD real de Podman (spec-08). */
@MicronautTest(transactional = false)
class AccountServiceTest {

    @Inject
    AccountService service;

    @Inject
    AccountRepository repository;

    private CreateAccountRequest request(String accountNumber, String currency,
                                         AccountType type, BigDecimal initialBalance) {
        return new CreateAccountRequest(accountNumber, currency, type, initialBalance);
    }

    @Test
    void createUsesOwnerAndDefaultsBalanceToZero() {
        UUID owner = UUID.randomUUID();
        String number = "AS-" + UUID.randomUUID();

        Account created = service.create(request(number, "EUR", AccountType.SAVINGS, null), owner);

        assertNotNull(created.id());
        assertEquals(owner, created.ownerId());
        assertEquals(number, created.accountNumber());
        assertEquals("EUR", created.currency());
        assertEquals(AccountType.SAVINGS, created.accountType());
        assertEquals(AccountStatus.ACTIVE, created.status());
        assertNotNull(created.createdAt());
        assertEquals(0, BigDecimal.ZERO.compareTo(created.balance()));
        assertTrue(repository.existsByAccountNumber(number));
    }

    @Test
    void createPersistsProvidedInitialBalanceAndCurrency() {
        UUID owner = UUID.randomUUID();
        Account created = service.create(
                request("AS-" + UUID.randomUUID(), "USD", AccountType.CHECKING,
                        new BigDecimal("123.45")),
                owner);

        Account stored = repository.findById(created.id()).orElseThrow();
        assertEquals(0, new BigDecimal("123.45").compareTo(stored.balance()));
        assertEquals("USD", stored.currency());
        assertEquals(owner, stored.ownerId());
    }

    @Test
    void createRejectsDuplicateAccountNumber() {
        UUID owner = UUID.randomUUID();
        String number = "AS-" + UUID.randomUUID();
        service.create(request(number, "EUR", AccountType.CHECKING, BigDecimal.ZERO), owner);

        DuplicateResourceException error = assertThrows(DuplicateResourceException.class,
                () -> service.create(request(number, "EUR", AccountType.SAVINGS, BigDecimal.TEN),
                                     UUID.randomUUID()));

        assertEquals(HttpStatus.CONFLICT, error.status());
        assertTrue(error.getMessage().contains(number));
    }

    @Test
    void findAllByOwnerReturnsOnlyThatOwnersAccounts() {
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        Account mine = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING, BigDecimal.ZERO), owner);
        Account foreign = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING, BigDecimal.ZERO), other);

        List<Account> found = service.findAllByOwner(owner);

        assertEquals(1, found.size());
        assertEquals(mine.id(), found.getFirst().id());
        assertFalse(found.stream().anyMatch(a -> a.id().equals(foreign.id())));
    }

    @Test
    void findByIdForOwnerRejectsMissingAndForeignAccounts() {
        UUID owner = UUID.randomUUID();
        Account mine = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING, BigDecimal.ZERO), owner);
        Account foreign = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING, BigDecimal.ZERO),
                UUID.randomUUID());

        assertEquals(mine.id(), service.findByIdForOwner(mine.id(), owner).id());

        AccountNotFoundException missing = assertThrows(AccountNotFoundException.class,
                () -> service.findByIdForOwner(UUID.randomUUID(), owner));
        assertEquals(HttpStatus.NOT_FOUND, missing.status());

        AccountNotFoundException notOwned = assertThrows(AccountNotFoundException.class,
                () -> service.findByIdForOwner(foreign.id(), owner));
        assertEquals(HttpStatus.NOT_FOUND, notOwned.status());
    }

    @Test
    void depositAndWithdrawUpdateTheStoredBalance() {
        UUID owner = UUID.randomUUID();
        Account account = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING,
                        new BigDecimal("100.00")),
                owner);

        Account deposited = service.deposit(account.id(), new BigDecimal("25.50"));
        assertEquals(0, new BigDecimal("125.50").compareTo(deposited.balance()));
        assertEquals(0, new BigDecimal("125.50")
                .compareTo(repository.findById(account.id()).orElseThrow().balance()));

        Account withdrawn = service.withdraw(account.id(), new BigDecimal("5.50"));
        assertEquals(0, new BigDecimal("120.00").compareTo(withdrawn.balance()));
        assertEquals(0, new BigDecimal("120.00")
                .compareTo(repository.findById(account.id()).orElseThrow().balance()));
    }

    @Test
    void withdrawBeyondBalanceThrowsAndLeavesBalanceUntouched() {
        UUID owner = UUID.randomUUID();
        Account account = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING,
                        new BigDecimal("10.00")),
                owner);

        InsufficientFundsException error = assertThrows(InsufficientFundsException.class,
                () -> service.withdraw(account.id(), new BigDecimal("50.00")));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertEquals(0, new BigDecimal("10.00")
                .compareTo(repository.findById(account.id()).orElseThrow().balance()));
    }

    @Test
    void deleteRequiresZeroBalanceAndRemovesOwnedAccount() {
        UUID owner = UUID.randomUUID();
        Account withBalance = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING,
                        new BigDecimal("10.00")),
                owner);
        Account empty = service.create(
                request("AS-" + UUID.randomUUID(), "EUR", AccountType.CHECKING, BigDecimal.ZERO),
                owner);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.delete(withBalance.id(), owner));
        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertTrue(repository.existsById(withBalance.id()));

        service.delete(empty.id(), owner);
        assertFalse(repository.existsById(empty.id()));
        assertThrows(AccountNotFoundException.class, () -> service.delete(empty.id(), owner));
    }
}
