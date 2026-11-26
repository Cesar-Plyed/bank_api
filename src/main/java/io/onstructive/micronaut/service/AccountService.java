package io.onstructive.micronaut.service;

import io.onstructive.micronaut.dto.*;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.repository.AccountRepository;
import jakarta.inject.Singleton;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Singleton
public class AccountService {

    private final AccountRepository repo;

    public AccountService(AccountRepository repo) {
        this.repo = repo;
    }

    /** Solo las cuentas del dueño autenticado (spec-03). */
    public List<Account> findAllByOwner(UUID ownerId) {
        return repo.findByOwnerId(ownerId);
    }

    /** El owner nunca viene del body: se toma del JWT (spec-03). */
    public Account create(CreateAccountRequest req, UUID ownerId) {
        if (repo.existsByAccountNumber(req.accountNumber()))
            throw new IllegalArgumentException("Account number already exists");
        BigDecimal initialBalance = req.initialBalance() == null
                ? BigDecimal.ZERO : req.initialBalance();
        return repo.save(Account.create(ownerId, req.accountNumber(), req.currency(),
                                        req.accountType(), initialBalance));
    }

    /** Una cuenta inexistente o ajena se trata igual: el controller responde 404. */
    public Account findByIdForOwner(UUID id, UUID ownerId) {
        return repo.findById(id)
                .filter(account -> account.ownerId().equals(ownerId))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + id));
    }

    // Se conservan para la spec-04 (/api/transactions).
    public Account deposit(UUID id, BigDecimal amount) {
        Account acc = repo.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Account not found: " + id));
        Account updated = new Account(acc.id(), acc.ownerId(), acc.accountNumber(),
                                      acc.currency(), acc.balance().add(amount),
                                      acc.accountType(), acc.status(), acc.createdAt());
        return repo.update(updated);
    }

    public Account withdraw(UUID id, BigDecimal amount) {
        Account acc = repo.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Account not found: " + id));
        if (acc.balance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient funds");
        Account updated = new Account(acc.id(), acc.ownerId(), acc.accountNumber(),
                                      acc.currency(), acc.balance().subtract(amount),
                                      acc.accountType(), acc.status(), acc.createdAt());
        return repo.update(updated);
    }

    public void delete(UUID id, UUID ownerId) {
        Account acc = findByIdForOwner(id, ownerId);
        if (acc.balance().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        repo.deleteById(id);
    }
}
