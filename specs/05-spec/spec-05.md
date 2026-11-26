# Spec-05 — Manejo consistente de errores y validación

## Objetivo
Unificar el payload de errores y los códigos HTTP, sustituyendo los `try/catch` dispersos en los controllers por excepciones de dominio.

## Alcance
- `exception/`: `BankException` (base con `HttpStatus`), `AccountNotFoundException` (404), `InsufficientFundsException` (400), `DuplicateResourceException` (409) y `BusinessRuleException` (400), o equivalentes claros.
- Reescribir `GlobalExceptionHandler`: mapear dominio a su status, `ConstraintViolationException` a 400 con `fieldErrors`, y cualquier otra excepción a 500 **sin exponer stack trace**. Payload único: `{ "error", "message", "path", "timestamp", "fieldErrors"? }`.
- Servicios y controllers: lanzar excepciones de dominio en lugar de `IllegalArgumentException`/`IllegalStateException` y quitar los `try/catch` que traducían a 4xx. La autenticación (401) y `@PermitAll`/`@Secured` quedan intactos.
- Tests: código y payload exactos para not found (404), fondos insuficientes (400), duplicado (409) y body inválido (400 con `fieldErrors`). Si es factible, un caso de error interno (500 sin stack trace).
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md` con la tabla de códigos y el payload.

## Fuera de alcance
Rate limiting y observabilidad (spec-06), suite completa y cobertura (spec-07), commits.

## Criterios de aceptación
1. Todo error de `/api/**` usa el mismo payload; los 19 tests existentes siguen pasando.
2. Códigos: 400 validación/regla de negocio, 401 autenticación, 404 inexistente/ajeno, 409 duplicado, 500 inesperado.
3. Se añaden al menos 4 tests nuevos de errores (404, 400, 409 y validación con `fieldErrors`).
4. `./gradlew test` pasa: 0 failures/errors.
5. Notas actualizadas con la tabla de códigos y el payload.

## Comandos de verificación
`./gradlew test --tests "*Error*Test"` (si aplica) · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
