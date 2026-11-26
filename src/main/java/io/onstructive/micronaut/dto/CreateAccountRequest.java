package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.onstructive.micronaut.model.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Body de {@code POST /api/accounts} (spec-03); no incluye {@code ownerId}. */
@Serdeable
public record CreateAccountRequest(
    @NotBlank String accountNumber,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
    @NotNull AccountType accountType,
    @PositiveOrZero BigDecimal initialBalance
) {}
