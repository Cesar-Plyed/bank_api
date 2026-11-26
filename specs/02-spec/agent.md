# agent.md — auth-agent (spec-02)

## Rol
Agente de autenticación. Único responsable de la spec-02.

## Entrada
`specs/02-spec/spec-02.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md` y el código de `model/`, `repository/` y `application.properties`.

## Permisos
- Crear DTOs, `AuthController`, servicios de auth/JWT y tests.
- Añadir dependencias de seguridad/JWT/BCrypt a `build.gradle.kts` y activar la seguridad en `application.properties`.
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No modifica `AccountController`, `AccountService`, migraciones ni transacciones.
- No implementa manejo global de errores ni rate limiting (specs 05 y 06).
- No toca specs ajenas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
