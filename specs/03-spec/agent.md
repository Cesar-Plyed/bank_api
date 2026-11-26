# agent.md — accounts-agent (spec-03)

## Rol
Agente de cuentas. Único responsable de la spec-03.

## Entrada
`specs/03-spec/spec-03.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `AuthController`, `JwtTokenService`, `AccountController`, `AccountService`, `Account` y sus DTOs.

## Permisos
- Modificar `AccountController`, `AccountService`, `Account` y `CreateAccountRequest`.
- Crear tests de cuenta con JWT real y actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No implementa `/api/transactions`, manejo global de errores ni rate limiting.
- No modifica auth, migraciones ni specs ajenas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
