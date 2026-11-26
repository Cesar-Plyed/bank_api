# agent.md — ratelimit-agent (spec-07)

## Rol
Agente de limitación de peticiones. Único responsable de la spec-07.

## Entrada
`specs/07-spec/spec-07.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `ErrorPayload`, `application.properties` y `build.gradle.kts`.

## Permisos
- Crear el filtro, la configuración, `application-test.yml`, los tests y actualizar notas; añadir el código `RATE_LIMIT_EXCEEDED` a `ErrorPayload`.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No modifica reglas de seguridad JWT, migraciones ni specs ajenas.
- No añade cobertura/JaCoCo ni empaquetado. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
