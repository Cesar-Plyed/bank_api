package io.onstructive.micronaut.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/** Body de {@code POST /api/transactions/transfer} (spec-04). */
@Serdeable
public record TransferRequest(
    @NotNull UUID fromAccountId,
    @NotNull UUID toAccountId,
    @NotNull @Positive BigDecimal amount,
    @Nullable String description
) {}
