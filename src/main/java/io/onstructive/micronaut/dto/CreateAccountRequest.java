package io.onstructive.micronaut.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
 
@Serdeable
public record CreateAccountRequest(
    @NotBlank String accountNumber,
    @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency
) {}
