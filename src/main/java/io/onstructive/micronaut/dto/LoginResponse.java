package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;

/** Respuesta de login con el JWT firmado (spec-02). */
@Serdeable
public record LoginResponse(
    String accessToken,
    String tokenType,
    int expiresIn,
    UserResponse user
) {}
