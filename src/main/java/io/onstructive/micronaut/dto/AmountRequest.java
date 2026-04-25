package io.onstructive.micronaut.dto;

import java.math.BigDecimal;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Positive;

@Serdeable
public record AmountRequest(
    @Positive BigDecimal amount
) {}

