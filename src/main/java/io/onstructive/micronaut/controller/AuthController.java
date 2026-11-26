package io.onstructive.micronaut.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.authentication.AuthenticationResponse;
import io.micronaut.security.authentication.UsernamePasswordCredentials;
import io.onstructive.micronaut.dto.LoginRequest;
import io.onstructive.micronaut.dto.LoginResponse;
import io.onstructive.micronaut.dto.RegisterRequest;
import io.onstructive.micronaut.dto.UserResponse;
import io.onstructive.micronaut.exception.ErrorPayload;
import io.onstructive.micronaut.model.User;
import io.onstructive.micronaut.repository.UserRepository;
import io.onstructive.micronaut.security.JwtTokenService;
import io.onstructive.micronaut.security.PasswordEncoder;
import io.onstructive.micronaut.security.UserAuthenticationProvider;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;

/** Registro y login JWT: ambos endpoints públicos (spec-02). */
@Controller("/api/auth")
@ExecuteOn(TaskExecutors.IO)
public class AuthController {

    /** Código estable del payload único para credenciales inválidas (spec-05). */
    private static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserAuthenticationProvider authenticationProvider;
    private final JwtTokenService jwtTokenService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          UserAuthenticationProvider authenticationProvider,
                          JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProvider = authenticationProvider;
        this.jwtTokenService = jwtTokenService;
    }

    @Post("/register")
    @PermitAll
    public HttpResponse<?> register(@Body @Valid RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())
                || userRepository.existsByEmail(request.email())) {
            return HttpResponse.status(HttpStatus.CONFLICT)
                    .body(ErrorPayload.of(ErrorPayload.CONFLICT,
                            "username or email already exists", "/api/auth/register"));
        }

        User user = new User(
                UUID.randomUUID(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                Instant.now());
        userRepository.save(user);
        return HttpResponse.created(UserResponse.from(user));
    }

    @Post("/login")
    @PermitAll
    public HttpResponse<?> login(@Body @Valid LoginRequest request) {
        AuthenticationResponse result = authenticationProvider.authenticate(
                (HttpRequest<?>) null,
                new UsernamePasswordCredentials(request.username(), request.password()));

        if (!result.isAuthenticated()) {
            return HttpResponse.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorPayload.of(AUTHENTICATION_FAILED,
                            "invalid credentials", "/api/auth/login"));
        }

        Authentication authentication = result.getAuthentication().orElseThrow();
        String token = jwtTokenService.generateToken(authentication);

        Object userId = authentication.getAttributes().get("userId");
        Object email = authentication.getAttributes().get("email");
        UserResponse user = new UserResponse(
                userId == null ? null : UUID.fromString(userId.toString()),
                authentication.getName(),
                email == null ? null : email.toString());

        LoginResponse body = new LoginResponse(
                token, "Bearer", JwtTokenService.EXPIRES_IN_SECONDS, user);
        return HttpResponse.ok(body);
    }
}
