package io.onstructive.micronaut.model;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("transactions")
public record Transaction(
    @Id UUID id,
    UUID fromAccountId,
    UUID toAccountId,
    @Positive BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull TransactionStatus status,
    @NotNull Instant timestamp,
    String description
) {}
