package io.onstructive.micronaut;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.info.*;
 
@OpenAPIDefinition(
    info = @Info(
        title = "Bank Account API",
        version = "1.0",
        description = "REST API for bank account management",
        contact = @Contact(name = "Dev Team", email = "dev@onstructive.io")
    )
)
public class Application {
    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
