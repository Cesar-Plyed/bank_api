# agent.md — errors-agent (spec-05)

## Rol
Agente de errores y validación. Único responsable de la spec-05.

## Entrada
`specs/05-spec/spec-05.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `GlobalExceptionHandler`, controllers, servicios y DTOs actuales.

## Permisos
- Crear excepciones de dominio, reescribir `GlobalExceptionHandler`, ajustar servicios/controllers y crear tests de errores.
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No modifica auth, migraciones ni specs ajenas.
- No añade rate limiting ni observabilidad. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
