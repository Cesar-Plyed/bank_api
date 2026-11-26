package io.onstructive.micronaut;

import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.dto.CreateAccountRequest;
import io.onstructive.micronaut.exception.AccountNotFoundException;
import io.onstructive.micronaut.exception.BusinessRuleException;
import io.onstructive.micronaut.exception.InsufficientFundsException;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountType;
import io.onstructive.micronaut.model.Transaction;
import io.onstructive.micronaut.model.TransactionStatus;
import io.onstructive.micronaut.model.TransactionType;
import io.onstructive.micronaut.model.User;
import io.onstructive.micronaut.repository.AccountRepository;
import io.onstructive.micronaut.repository.TransactionRepository;
import io.onstructive.micronaut.repository.UserRepository;
import io.onstructive.micronaut.service.AccountService;
import io.onstructive.micronaut.service.TransactionService;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tests de servicio de transacciones contra la BD real de Podman (spec-08). */
@MicronautTest(transactional = false)
class TransactionServiceTest {

    @Inject
    TransactionService service;

    @Inject
    AccountService accountService;

    @Inject
    AccountRepository accountRepository;

    @Inject
    TransactionRepository transactionRepository;

    @Inject
    UserRepository userRepository;

    /** Dueño real: V3 exige que accounts.owner_id exista en users. */
    private UUID owner() {
        String suffix = UUID.randomUUID().toString();
        UUID id = UUID.randomUUID();
        userRepository.save(new User(id, "ts-owner-" + suffix, "ts-owner-" + suffix + "@example.com",
                                     "hash", Instant.now()));
        return id;
    }

    private Account account(UUID owner, String balance) {
        return accountService.create(
                new CreateAccountRequest("TS-" + UUID.randomUUID(), "EUR",
                                         AccountType.CHECKING, new BigDecimal(balance)),
                owner);
    }

    private BigDecimal balance(UUID accountId) {
        return accountRepository.findById(accountId).orElseThrow().balance();
    }

    private void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), actual.toPlainString());
    }

    @Test
    void depositIncreasesBalanceAndSavesOneCompletedTransaction() {
        UUID owner = owner();
        Account account = account(owner, "100.00");
        long before = transactionRepository.count();

        Transaction tx = service.deposit(account.id(), new BigDecimal("25.50"), owner, "salary");

        assertNotNull(tx.id());
        assertEquals(TransactionType.DEPOSIT, tx.type());
        assertEquals(TransactionStatus.COMPLETED, tx.status());
        assertMoney("25.50", tx.amount());
        assertNull(tx.fromAccountId());
        assertEquals(account.id(), tx.toAccountId());
        assertEquals("salary", tx.description());
        assertNotNull(tx.timestamp());
        assertMoney("125.50", balance(account.id()));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void withdrawDecreasesBalanceAndSavesOneCompletedTransaction() {
        UUID owner = owner();
        Account account = account(owner, "100.00");
        long before = transactionRepository.count();

        Transaction tx = service.withdraw(account.id(), new BigDecimal("40.00"), owner, null);

        assertEquals(TransactionType.WITHDRAWAL, tx.type());
        assertEquals(TransactionStatus.COMPLETED, tx.status());
        assertEquals(account.id(), tx.fromAccountId());
        assertNull(tx.toAccountId());
        assertNull(tx.description());
        assertMoney("60.00", balance(account.id()));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void withdrawWithInsufficientFundsRollsBackBalanceAndTransaction() {
        UUID owner = owner();
        Account account = account(owner, "10.00");
        long before = transactionRepository.count();

        InsufficientFundsException error = assertThrows(InsufficientFundsException.class,
                () -> service.withdraw(account.id(), new BigDecimal("50.00"), owner, "fail"));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertMoney("10.00", balance(account.id()));
        assertEquals(before, transactionRepository.count());
    }

    @Test
    void transferMovesFundsBetweenOwnedAccountsAndSavesOneTransaction() {
        UUID owner = owner();
        Account from = account(owner, "100.00");
        Account to = account(owner, "50.00");
        long before = transactionRepository.count();

        Transaction tx = service.transfer(from.id(), to.id(), new BigDecimal("30.00"),
                                          owner, "rent");

        assertEquals(TransactionType.TRANSFER, tx.type());
        assertEquals(TransactionStatus.COMPLETED, tx.status());
        assertEquals(from.id(), tx.fromAccountId());
        assertEquals(to.id(), tx.toAccountId());
        assertEquals("rent", tx.description());
        assertMoney("70.00", balance(from.id()));
        assertMoney("80.00", balance(to.id()));
        assertEquals(before + 1, transactionRepository.count());
    }

    @Test
    void transferWithInsufficientFundsRollsBackBothBalancesAndTransaction() {
        UUID owner = owner();
        Account from = account(owner, "10.00");
        Account to = account(owner, "0.00");
        long before = transactionRepository.count();

        InsufficientFundsException error = assertThrows(InsufficientFundsException.class,
                () -> service.transfer(from.id(), to.id(), new BigDecimal("100.00"), owner, "fail"));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertMoney("10.00", balance(from.id()));
        assertMoney("0.00", balance(to.id()));
        assertEquals(before, transactionRepository.count());
    }

    @Test
    void transferToTheSameAccountIsRejected() {
        UUID owner = owner();
        Account account = account(owner, "100.00");
        long before = transactionRepository.count();

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.transfer(account.id(), account.id(), BigDecimal.ONE, owner, null));

        assertEquals(HttpStatus.BAD_REQUEST, error.status());
        assertMoney("100.00", balance(account.id()));
        assertEquals(before, transactionRepository.count());
    }

    @Test
    void foreignAndMissingAccountsAreRejectedByEveryOperation() {
        UUID owner = owner();
        Account mine = account(owner, "100.00");
        Account foreign = account(owner(), "20.00");
        long before = transactionRepository.count();

        assertThrows(AccountNotFoundException.class,
                () -> service.deposit(foreign.id(), BigDecimal.ONE, owner, null));
        assertThrows(AccountNotFoundException.class,
                () -> service.withdraw(foreign.id(), BigDecimal.ONE, owner, null));
        assertThrows(AccountNotFoundException.class,
                () -> service.transfer(mine.id(), foreign.id(), BigDecimal.ONE, owner, null));
        assertThrows(AccountNotFoundException.class,
                () -> service.history(foreign.id(), owner));
        assertThrows(AccountNotFoundException.class,
                () -> service.deposit(UUID.randomUUID(), BigDecimal.ONE, owner, null));
        assertThrows(AccountNotFoundException.class,
                () -> service.history(UUID.randomUUID(), owner));

        assertMoney("100.00", balance(mine.id()));
        assertMoney("20.00", balance(foreign.id()));
        assertEquals(before, transactionRepository.count());
    }

    @Test
    void historyReturnsMovementsInBothDirections() {
        UUID owner = owner();
        Account a = account(owner, "100.00");
        Account b = account(owner, "0.00");

        service.deposit(a.id(), new BigDecimal("20.00"), owner, null);
        service.transfer(a.id(), b.id(), new BigDecimal("30.00"), owner, null);

        List<Transaction> historyA = service.history(a.id(), owner);
        assertEquals(2, historyA.size());
        assertTrue(historyA.stream().anyMatch(tx -> tx.type() == TransactionType.DEPOSIT
                                                    && a.id().equals(tx.toAccountId())));
        assertTrue(historyA.stream().anyMatch(tx -> tx.type() == TransactionType.TRANSFER
                                                    && a.id().equals(tx.fromAccountId())));

        List<Transaction> historyB = service.history(b.id(), owner);
        assertEquals(1, historyB.size());
        assertEquals(TransactionType.TRANSFER, historyB.getFirst().type());
        assertEquals(b.id(), historyB.getFirst().toAccountId());
    }
}
