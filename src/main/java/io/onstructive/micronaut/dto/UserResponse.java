package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import io.onstructive.micronaut.model.User;
import java.util.UUID;

/** Datos públicos de un usuario; nunca expone el hash de la contraseña (spec-02). */
@Serdeable
public record UserResponse(UUID id, String username, String email) {

    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.username(), user.email());
    }
}
