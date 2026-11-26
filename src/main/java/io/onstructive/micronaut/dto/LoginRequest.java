package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;

/** Credenciales de entrada para el login (spec-02). */
@Serdeable
public record LoginRequest(
    @NotBlank String username,
    @NotBlank String password
) {}
