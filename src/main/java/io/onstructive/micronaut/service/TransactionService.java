package io.onstructive.micronaut.service;

import io.micronaut.transaction.annotation.Transactional;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.Transaction;
import io.onstructive.micronaut.model.TransactionStatus;
import io.onstructive.micronaut.model.TransactionType;
import io.onstructive.micronaut.repository.AccountRepository;
import io.onstructive.micronaut.repository.TransactionRepository;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Operaciones ACID sobre cuentas (spec-04).
 *
 * <p>Cada operación de escritura es una única transacción: verificación de
 * propiedad, validación de fondos, actualización de balances y registro de la
 * {@link Transaction} ocurren en la misma transacción. Cualquier excepción de
 * dominio provoca rollback, por lo que no quedan balances a medias ni filas
 * huérfanas. La propiedad se comprueba siempre contra el {@code userId} del JWT;
 * una cuenta ajena o inexistente se trata igual ({@link IllegalArgumentException},
 * que el controller traduce a 404).
 */
@Singleton
public class TransactionService {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public TransactionService(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    /** Cuenta existente y perteneciente al dueño autenticado; si no, 404 desde el controller. */
    private Account owned(UUID id, UUID ownerId) {
        return accounts.findById(id)
                .filter(account -> account.ownerId().equals(ownerId))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    /** Copia de la cuenta con un balance distinto, conservando el resto de los campos. */
    private Account withBalance(Account account, BigDecimal balance) {
        return new Account(account.id(), account.ownerId(), account.accountNumber(),
                           account.currency(), balance, account.accountType(),
                           account.status(), account.createdAt());
    }

    /** Registro de la transacción completada; siempre se crea dentro de la transacción activa. */
    private Transaction saveTransaction(UUID from, UUID to, BigDecimal amount,
                                        TransactionType type, String description) {
        return transactions.save(new Transaction(UUID.randomUUID(), from, to, amount, type,
                                                 TransactionStatus.COMPLETED, Instant.now(),
                                                 description));
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction deposit(UUID accountId, BigDecimal amount, UUID ownerId, String description) {
        Account account = owned(accountId, ownerId);
        accounts.update(withBalance(account, account.balance().add(amount)));
        return saveTransaction(null, accountId, amount, TransactionType.DEPOSIT, description);
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction withdraw(UUID accountId, BigDecimal amount, UUID ownerId, String description) {
        Account account = owned(accountId, ownerId);
        if (account.balance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        accounts.update(withBalance(account, account.balance().subtract(amount)));
        return saveTransaction(accountId, null, amount, TransactionType.WITHDRAWAL, description);
    }

    @Transactional(rollbackFor = Exception.class)
    public Transaction transfer(UUID fromAccountId, UUID toAccountId, BigDecimal amount,
                                UUID ownerId, String description) {
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalStateException("Cannot transfer to the same account");
        }
        Account source = owned(fromAccountId, ownerId);
        Account target = owned(toAccountId, ownerId);
        if (source.balance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds");
        }
        accounts.update(withBalance(source, source.balance().subtract(amount)));
        accounts.update(withBalance(target, target.balance().add(amount)));
        return saveTransaction(fromAccountId, toAccountId, amount,
                               TransactionType.TRANSFER, description);
    }

    /** Historial de una cuenta propia, en ambos sentidos (como origen y como destino). */
    @Transactional(readOnly = true)
    public List<Transaction> history(UUID accountId, UUID ownerId) {
        owned(accountId, ownerId);
        return transactions.findByFromAccountIdOrToAccountId(accountId, accountId);
    }
}
