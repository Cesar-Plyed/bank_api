package io.onstructive.micronaut.model;

import io.micronaut.core.annotation.Nullable;
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
    @Nullable UUID fromAccountId,
    @Nullable UUID toAccountId,
    @Positive BigDecimal amount,
    @NotNull TransactionType type,
    @NotNull TransactionStatus status,
    @NotNull Instant timestamp,
    @Nullable String description
) {}
