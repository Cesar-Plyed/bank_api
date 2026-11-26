# Spec-03 — Cuentas con dueño bajo `/api/accounts`

## Objetivo
Migrar el CRUD legacy de cuentas a `/api/accounts`, autenticado y limitado al usuario dueño.

## Alcance
- `AccountController` pasa de `/accounts` a `/api/accounts` con:
  - `POST /api/accounts` → 201: `accountNumber`, `accountType` (`SAVINGS`/`CHECKING`), `initialBalance` (default 0), `currency`; el `ownerId` se toma del JWT (atributo `userId`), nunca del body.
  - `GET /api/accounts` → solo cuentas del usuario autenticado.
  - `GET /api/accounts/{id}` → solo si pertenece al usuario; si no existe o es de otro → 404.
  - `DELETE /api/accounts/{id}` → solo si pertenece al usuario y `balance == 0`.
- Quitar de `AccountController` los endpoints legacy `deposit`/`withdraw` (se reimplementarán en `/api/transactions` en la spec-04).
- `CreateAccountRequest` se amplía con `accountType` e `initialBalance` (`@PositiveOrZero`); `Account.create(...)` recibe el owner real y se elimina `DEFAULT_OWNER_ID`.
- `AccountService`: `create(req, ownerId)`, `findAllByOwner(ownerId)`, `findByIdForOwner(id, ownerId)`, `delete(id, ownerId)`; se conservan `deposit`/`withdraw` para la spec-04.
- Tests `@MicronautTest(transactional=false)` con JWT real (registro + login helper): crear cuenta, listar solo las propias, get de cuenta ajena → 404, borrar con saldo → 4xx, y acceso sin token → 401.
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md` con el nuevo prefijo y el contexto de propiedad.

## Fuera de alcance
- Endpoints `/api/transactions` (spec-04), manejo global de errores (spec-05), observabilidad/rate limiting (spec-06), suite completa (spec-07), commits.

## Criterios de aceptación
1. `/accounts` desaparece; `/api/accounts` responde 401 sin JWT.
2. Una cuenta creada tiene `ownerId` igual al `userId` del token; `DEFAULT_OWNER_ID` ya no existe.
3. Un usuario no ve ni puede borrar cuentas de otro (404).
4. `./gradlew test` pasa con los tests nuevos y los existentes: 0 failures/errors.
5. Notas actualizadas con `/api/accounts` y el esquema de propiedad.

## Comandos de verificación
`./gradlew test --tests "*Account*Test"` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
