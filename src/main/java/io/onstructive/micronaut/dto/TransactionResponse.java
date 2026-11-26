package io.onstructive.micronaut.dto;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;
import io.onstructive.micronaut.model.Transaction;
import io.onstructive.micronaut.model.TransactionStatus;
import io.onstructive.micronaut.model.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de las operaciones de transacción (spec-04).
 *
 * <p>{@code fromAccount}/{@code toAccount} son los IDs de las cuentas origen y
 * destino. Un depósito no tiene origen y un retiro no tiene destino, por lo que
 * esos campos son opcionales en esos casos; {@code description} siempre es opcional.
 */
@Serdeable
public record TransactionResponse(
    UUID id,
    @Nullable UUID fromAccount,
    @Nullable UUID toAccount,
    BigDecimal amount,
    TransactionType type,
    TransactionStatus status,
    Instant timestamp,
    @Nullable String description
) {
    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
            transaction.id(),
            transaction.fromAccountId(),
            transaction.toAccountId(),
            transaction.amount(),
            transaction.type(),
            transaction.status(),
            transaction.timestamp(),
            transaction.description()
        );
    }
}
