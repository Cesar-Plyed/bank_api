package io.onstructive.micronaut.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/** Body de {@code POST /api/transactions/deposit|withdraw} (spec-04). */
@Serdeable
public record TransactionRequest(
    @NotNull UUID accountId,
    @NotNull @Positive BigDecimal amount,
    @Nullable String description
) {}
