# Spec-01 — Dominio y persistencia (PostgreSQL + Flyway)

## Objetivo
Definir el modelo de dominio y dejar el esquema gestionado por Flyway sobre PostgreSQL 15 en Podman.

## Alcance
- Entidades `User`, `Account`, `Transaction` y enums `AccountType`, `AccountStatus`, `TransactionType`, `TransactionStatus` en `model/`. IDs `UUID` asignados en código (sin `@GeneratedValue`), `BigDecimal` para dinero, `Instant` para fechas.
- `Account` incorpora `ownerId`, `accountType`, `status`, `createdAt`. `Transaction` incorpora `fromAccountId`, `toAccountId`, `amount`, `type`, `status`, `timestamp`, `description`.
- Repositorios: `UserRepository` (`findByUsername`, `existsByUsername`, `existsByEmail`), `AccountRepository` (`findByOwnerId`, `existsByAccountNumber`), `TransactionRepository` (`findByFromAccountIdOrToAccountId`). Todos con `Dialect.POSTGRES`.
- Migraciones `V1__CreateTables.sql` y `V2__AddIndexes.sql` en `src/main/resources/db/migration/`; `schema-generate=NONE` para que Flyway sea dueño del esquema.
- PostgreSQL 15 en Podman rootless (`127.0.0.1:5432`, db `bank_api`, usuario `bank_user`, password `bank_pass`); añadir `org.postgresql:postgresql` a `build.gradle.kts`; datasource con valores por entorno y defaults.
- Ajuste **mínimo** de `AccountService`/`AccountController` para que compile con el nuevo `Account` (sin cambiar comportamiento).
- Test mínimo `@MicronautTest` de repositorio: guardar y leer un `User` y una `Account` contra la BD de Podman.
- Documentar en `NOTES.md`, `MEMORY.md` y `AGENTS.md` los comandos exactos para iniciar/parar la BD y el `schema-generate=NONE`.

## Fuera de alcance
- Autenticación/JWT (spec-02), endpoints de transacciones (spec-04), suite completa de tests (spec-07), commits/push/borrados.

## Criterios de aceptación
1. `podman ps` muestra el contenedor `bank-api-db` activo y `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` lista `users`, `accounts` y `transactions`.
2. `./gradlew classes` compila: las nuevas firmas de `Account` no rompen `AccountService` ni `AccountController`.
3. `./gradlew test` pasa con el test de repositorio nuevo y el `BankaccountTest` existente.
4. Las tablas y los índices fueron creados por Flyway desde el primer arranque del contenedor (sin `schema-generate`).
5. `AGENTS.md`, `NOTES.md` y `MEMORY.md` contienen los comandos Podman y la nota `schema-generate=NONE`.

## Comandos de verificación
`podman ps` · `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` · `./gradlew classes` · `./gradlew test`

## Definition of Done
Reportar archivos creados/modificados, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
