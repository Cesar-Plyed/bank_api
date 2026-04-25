package io.onstructive.micronaut.model;

import io.micronaut.data.annotation.GeneratedValue;
import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.UUID;
 
@Serdeable
@MappedEntity("accounts")
public record Account(
    @Id @GeneratedValue UUID id,
    @NotBlank String accountNumber,
    @NotBlank String currency,
    @PositiveOrZero BigDecimal balance
) {
    // Factory method para crear cuenta nueva
    public static Account create(String accountNumber, String currency) {
        return new Account(null, accountNumber, currency, BigDecimal.ZERO);
    }
}
