package io.onstructive.micronaut.controller;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.rules.SecurityRule;
import io.onstructive.micronaut.dto.TransactionRequest;
import io.onstructive.micronaut.dto.TransactionResponse;
import io.onstructive.micronaut.dto.TransferRequest;
import io.onstructive.micronaut.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

/**
 * Transacciones atómicas e historial por cuenta (spec-04).
 *
 * <p>La base {@code /api} permite publicar aquí tanto {@code /api/transactions/*}
 * como {@code /api/accounts/{accountId}/transactions} con un único controller.
 * El dueño se lee siempre del claim {@code userId} del JWT, nunca del body.
 */
@Controller("/api")
@Secured(SecurityRule.IS_AUTHENTICATED)
@ExecuteOn(TaskExecutors.IO)
@Tag(name = "Transactions", description = "Atomic deposits, withdrawals and transfers")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
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

    @Post("/transactions/deposit")
    @Operation(summary = "Deposit into an owned account")
    public HttpResponse<TransactionResponse> deposit(@Body @Valid TransactionRequest request,
                                                     @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.created(TransactionResponse.from(service.deposit(
                request.accountId(), request.amount(), ownerId, request.description())));
    }

    @Post("/transactions/withdraw")
    @Operation(summary = "Withdraw from an owned account (400 if insufficient funds)")
    public HttpResponse<TransactionResponse> withdraw(@Body @Valid TransactionRequest request,
                                                      @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.created(TransactionResponse.from(service.withdraw(
                request.accountId(), request.amount(), ownerId, request.description())));
    }

    @Post("/transactions/transfer")
    @Operation(summary = "Transfer between two owned accounts, atomically")
    public HttpResponse<TransactionResponse> transfer(@Body @Valid TransferRequest request,
                                                      @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.created(TransactionResponse.from(service.transfer(
                request.fromAccountId(), request.toAccountId(), request.amount(),
                ownerId, request.description())));
    }

    @Get("/accounts/{accountId}/transactions")
    @Operation(summary = "History of an owned account, in both directions")
    public HttpResponse<List<TransactionResponse>> history(@PathVariable UUID accountId,
                                                           @Nullable Authentication authentication) {
        UUID ownerId = currentOwnerId(authentication);
        if (ownerId == null) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.ok(service.history(accountId, ownerId).stream()
                                       .map(TransactionResponse::from)
                                       .toList());
    }
}
