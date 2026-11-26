package io.onstructive.micronaut.security;

import jakarta.inject.Singleton;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Codificador de contraseñas con BCrypt (spec-02).
 *
 * <p>Micronaut Security 4 no aporta la interfaz {@code PasswordEncoder}, así que se
 * define un bean propre con jBCrypt. El coste fijo es 10 rondas; el hash guardado
 * empieza por {@code $2a$} / {@code $2b$}, nunca contiene la contraseña en claro.
 */
@Singleton
public class PasswordEncoder {

    private static final int ROUNDS = 10;

    public String encode(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(ROUNDS));
    }

    public boolean matches(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Hash corrupto o no BCrypt: credencial inválida, no un error 500.
            return false;
        }
    }
}
