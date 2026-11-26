package io.onstructive.micronaut.model;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("accounts")
public record Account(
    @Id UUID id,
    @NotNull UUID ownerId,
    @NotBlank String accountNumber,
    @NotBlank String currency,
    @PositiveOrZero BigDecimal balance,
    @NotNull AccountType accountType,
    @NotNull AccountStatus status,
    @NotNull Instant createdAt
) {
    /** Owner used by the legacy /accounts CRUD endpoint, which has no owner field yet. */
    public static final UUID DEFAULT_OWNER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static Account create(String accountNumber, String currency) {
        return new Account(UUID.randomUUID(), DEFAULT_OWNER_ID, accountNumber, currency,
                           BigDecimal.ZERO, AccountType.CHECKING, AccountStatus.ACTIVE,
                           Instant.now());
    }
}
