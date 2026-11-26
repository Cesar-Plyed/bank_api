package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de entrada para el registro de usuarios (spec-02). */
@Serdeable
public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 64) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 128) String password
) {}
