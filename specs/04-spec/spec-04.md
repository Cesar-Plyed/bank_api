# Spec-04 — Transacciones ACID en `/api/transactions`

## Objetivo
Depósitos, retiros y transferencias atómicos, con registro de `Transaction` e historial por cuenta.

## Alcance
- `TransactionController` con `@Secured`, `@ExecuteOn(TaskExecutors.IO)` y estas rutas exactas:
  - `POST /api/transactions/deposit` → 201 `TransactionResponse`.
  - `POST /api/transactions/withdraw` → 201 `TransactionResponse`.
  - `POST /api/transactions/transfer` → 201 `TransactionResponse`.
  - `GET /api/accounts/{accountId}/transactions` → 200 lista de movimientos de esa cuenta (ambos sentidos).
- DTOs `@Serdeable`: `TransactionRequest` (`accountId`, `amount`, `description`), `TransferRequest` (`fromAccountId`, `toAccountId`, `amount`, `description`), `TransactionResponse` (id, fromAccount, toAccount, amount, type, status, timestamp, description). `description` opcional.
- `TransactionService` con `@Transactional` en cada operación: verificar propiedad de las cuentas del `userId` del JWT, validar fondos, actualizar balances y guardar `Transaction` con `status=COMPLETED`, `type` acorde e `Instant.now()`.
- Una transferencia fallida (fondos insuficientes o cuenta destino ajena/inexistente) **no debe** dejar balances modificados ni filas de `Transaction`.
- Errores de dominio se traducen a 4xx en el controller; el manejo global de errores llega en la spec-05.
- Tests `@MicronautTest(transactional=false)` con JWT real: depósito, retiro, transferencia, historial en ambos sentidos, fondos insuficientes sin cambios, cuenta ajena → 404, y sin token → 401.
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md` con las rutas y las garantías ACID.

## Fuera de alcance
- Manejo global de errores (spec-05), observabilidad/rate limiting (spec-06), suite completa (spec-07), commits.

## Criterios de aceptación
1. Las 4 rutas responden según lo indicado; sin JWT → 401; cuenta ajena → 404.
2. Deposit/withdraw/transfer actualizan balances y crean exactamente una `Transaction` cada una.
3. Fondos insuficientes → 4xx, balances intactos y sin fila de transacción (rollback verificado por test).
4. `./gradlew test` pasa con los tests nuevos y los 12 existentes: 0 failures/errors.
5. Notas actualizadas con las rutas `/api/transactions` y el historial.

## Comandos de verificación
`./gradlew test --tests "*Transaction*Test"` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
