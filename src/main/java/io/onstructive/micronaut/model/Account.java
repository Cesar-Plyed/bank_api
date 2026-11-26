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
    /**
     * Crea una cuenta con un dueño real (spec-03): el {@code ownerId} se toma del
     * atributo {@code userId} del JWT, nunca de un valor por defecto ni del body.
     */
    public static Account create(UUID ownerId, String accountNumber, String currency,
                                 AccountType accountType, BigDecimal initialBalance) {
        return new Account(UUID.randomUUID(), ownerId, accountNumber, currency,
                           initialBalance, accountType, AccountStatus.ACTIVE,
                           Instant.now());
    }
}
