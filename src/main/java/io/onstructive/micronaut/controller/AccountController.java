package io.onstructive.micronaut.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.onstructive.micronaut.dto.CreateAccountRequest;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/** CRUD de cuentas limitado al dueño autenticado (spec-03). */
@Controller("/api/accounts")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
@Tag(name = "Accounts", description = "Bank Account operations")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    /** El ownerId se lee del claim {@code userId} del JWT; nunca del body. */
    private UUID currentOwnerId(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Object raw = authentication.getAttributes().get("userId");
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(raw.toString());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Get
    @Operation(summary = "List accounts owned by the authenticated user")
    public HttpResponse<List<Account>> list(@Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.ok(service.findAllByOwner(ownerId));
    }

    @Post
    @Operation(summary = "Create an account for the authenticated user")
    public HttpResponse<Account> create(@Body @Valid CreateAccountRequest req,
                                        @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.created(service.create(req, ownerId));
    }

    @Get("/{id}")
    @Operation(summary = "Get an owned account (404 if missing or owned by another user)")
    public HttpResponse<Account> get(@PathVariable UUID id,
                                     @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.ok(service.findByIdForOwner(id, ownerId));
    }

    @Delete("/{id}")
    @Operation(summary = "Delete an owned account only when its balance is zero")
    public HttpResponse<Void> delete(@PathVariable UUID id,
                                     @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        service.delete(id, ownerId);
        return HttpResponse.noContent();
    }
}
