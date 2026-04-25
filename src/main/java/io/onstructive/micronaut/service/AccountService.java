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
    public List<Account> findAll() {
        return (List<Account>) repo.findAll();
    }
 
    public Account create(CreateAccountRequest req) {
        if (repo.existsByAccountNumber(req.accountNumber()))
            throw new IllegalArgumentException("Account number already exists");
        return repo.save(Account.create(req.accountNumber(), req.currency()));
    }
 
    public Account deposit(UUID id, BigDecimal amount) {
        Account acc = repo.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Account not found: " + id));
        Account updated = new Account(acc.id(), acc.accountNumber(),
                                      acc.currency(), acc.balance().add(amount));
        return repo.update(updated);
    }
 
    public Account withdraw(UUID id, BigDecimal amount) {
        Account acc = repo.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Account not found: " + id));
        if (acc.balance().compareTo(amount) < 0)
            throw new IllegalStateException("Insufficient funds");
        Account updated = new Account(acc.id(), acc.accountNumber(),
                                      acc.currency(), acc.balance().subtract(amount));
        return repo.update(updated);
    }
 
    public void delete(UUID id) {
        Account acc = repo.findById(id).orElseThrow(
            () -> new IllegalArgumentException("Account not found: " + id));
        if (acc.balance().compareTo(BigDecimal.ZERO) != 0)
            throw new IllegalStateException("Cannot delete account with non-zero balance");
        repo.deleteById(id);
    }
}

