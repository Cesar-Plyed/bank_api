package io.onstructive.micronaut.security;

import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import jakarta.inject.Singleton;

/**
 * Firma el JWT con el secreto configurado (spec-02).
 *
 * <p>El subject ({@code sub}) es el username porque la autenticación se construye
 * con ese nombre; la expiración se fija en 3600 segundos.
 */
@Singleton
public class JwtTokenService {

    public static final int EXPIRES_IN_SECONDS = 3600;

    private final JwtTokenGenerator jwtTokenGenerator;

    public JwtTokenService(JwtTokenGenerator jwtTokenGenerator) {
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    public String generateToken(Authentication authentication) {
        return jwtTokenGenerator.generateToken(authentication, EXPIRES_IN_SECONDS)
                .orElseThrow(() -> new IllegalStateException("No se pudo generar el JWT"));
    }
}
