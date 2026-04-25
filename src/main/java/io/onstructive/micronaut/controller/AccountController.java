package io.onstructive.micronaut.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.onstructive.micronaut.dto.*;
import io.onstructive.micronaut.model.Account;
import io.onstructive.micronaut.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
 
@Controller("/accounts")
@Tag(name = "Accounts", description = "Bank Account operations")
public class AccountController {
 
    private final AccountService service;
 
    public AccountController(AccountService service) {
        this.service = service;
    }
 
    @Get
    @Operation(summary = "List all accounts")
    public List<Account> list() {
        return service.findAll();
    }
 
    @Post
    @Operation(summary = "Create a new account")
    public HttpResponse<Account> create(@Body @Valid CreateAccountRequest req) {
        return HttpResponse.created(service.create(req));
    }
 
    @Post("/{id}/deposit")
    @Operation(summary = "Deposit funds")
    public Account deposit(@PathVariable UUID id, @Body @Valid AmountRequest req) {
        return service.deposit(id, req.amount());
    }
 
    @Post("/{id}/withdraw")
    @Operation(summary = "Withdraw funds")
    public Account withdraw(@PathVariable UUID id, @Body @Valid AmountRequest req) {
        return service.withdraw(id, req.amount());
    }
 
    @Delete("/{id}")
    @Operation(summary = "Delete account (only if balance = 0)")
    public HttpResponse<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return HttpResponse.noContent();
    }
}

