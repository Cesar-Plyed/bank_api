package io.onstructive.micronaut;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.model.AccountStatus;
import io.onstructive.micronaut.model.AccountType;
import io.onstructive.micronaut.model.User;
import io.onstructive.micronaut.repository.AccountRepository;
import io.onstructive.micronaut.repository.UserRepository;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class RepositoryTest {

    @Inject
    UserRepository userRepository;

    @Inject
    AccountRepository accountRepository;

    @Test
    void savesAndReadsUserAndAccount() {
        UUID userId = UUID.randomUUID();
        String suffix = UUID.randomUUID().toString();
        User user = new User(userId, "user-" + suffix, suffix + "@example.com",
                             "hash", Instant.now());
        userRepository.save(user);

        assertTrue(userRepository.findByUsername(user.username()).isPresent());
        assertTrue(userRepository.existsByUsername(user.username()));
        assertTrue(userRepository.existsByEmail(user.email()));
        assertEquals(user.email(), userRepository.findByUsername(user.username()).orElseThrow().email());

        Account account = new Account(UUID.randomUUID(), userId, "ACC-" + suffix, "EUR",
                                      new BigDecimal("100.00"), AccountType.CHECKING,
                                      AccountStatus.ACTIVE, Instant.now());
        accountRepository.save(account);

        Account found = accountRepository.findByAccountNumber(account.accountNumber()).orElseThrow();
        assertEquals(userId, found.ownerId());
        assertEquals(AccountType.CHECKING, found.accountType());
        assertEquals(AccountStatus.ACTIVE, found.status());
        assertTrue(accountRepository.existsByAccountNumber(account.accountNumber()));
        assertEquals(1, accountRepository.findByOwnerId(userId).size());
    }
}
