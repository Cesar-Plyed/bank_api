# agent.md — transactions-agent (spec-04)

## Rol
Agente de transacciones. Único responsable de la spec-04.

## Entrada
`specs/04-spec/spec-04.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `AccountService`, `AccountController`, `Account`, `Transaction`, `TransactionRepository` y los DTOs existentes.

## Permisos
- Crear `TransactionController`, `TransactionService` y los DTOs de transacción.
- Crear tests HTTP con JWT real y actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No modifica auth, migraciones, manejo global de errores ni specs ajenas.
- No añade rate limiting ni observabilidad. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
