package io.onstructive.micronaut.model;

import io.micronaut.data.annotation.Id;
import io.micronaut.data.annotation.MappedEntity;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

@Serdeable
@MappedEntity("users")
public record User(
    @Id UUID id,
    @NotBlank String username,
    @NotBlank String email,
    @NotBlank String passwordHash,
    Instant createdAt
) {}
