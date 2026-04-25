package io.onstructive.micronaut.repository;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.repository.CrudRepository;
import io.onstructive.micronaut.model.Account;
import java.util.Optional;
import java.util.UUID;
 
@JdbcRepository(dialect = Dialect.H2)   // cambiar a POSTGRES en producción
public interface AccountRepository extends CrudRepository<Account, UUID> {
 
    Optional<Account> findByAccountNumber(String accountNumber);
 
    boolean existsByAccountNumber(String accountNumber);
}
