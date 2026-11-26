package io.onstructive.micronaut.security;

import io.micronaut.http.HttpRequest;
import io.micronaut.security.authentication.AuthenticationRequest;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.provider.AuthenticationProvider;
import io.onstructive.micronaut.model.User;
import io.onstructive.micronaut.repository.UserRepository;
import jakarta.inject.Singleton;
import java.util.Map;
import java.util.Optional;

/**
 * Valida las credenciales del login contra {@link UserRepository} (spec-02).
 *
 * <p>El subject del JWT será el username; el id y el email se viajan como atributos
 * de la autenticación para construir la respuesta de login sin una segunda lectura.
 */
@Singleton
public class UserAuthenticationProvider
        implements AuthenticationProvider<HttpRequest<?>, String, String> {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAuthenticationProvider(UserRepository userRepository,
                                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public AuthenticationResponse authenticate(
            HttpRequest<?> request,
            AuthenticationRequest<String, String> authenticationRequest) {

        String username = authenticationRequest.getIdentity();
        String password = authenticationRequest.getSecret();
        if (username == null || password == null) {
            return AuthenticationResponse.failure();
        }

        Optional<User> found = userRepository.findByUsername(username);
        if (found.isEmpty() || !passwordEncoder.matches(password, found.get().passwordHash())) {
            return AuthenticationResponse.failure();
        }

        User user = found.get();
        return AuthenticationResponse.success(
                user.username(),
                Map.of("userId", user.id().toString(), "email", user.email()));
    }
}
