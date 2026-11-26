# agent.md — observability-agent (spec-06)

## Rol
Agente de observabilidad. Único responsable de la spec-06.

## Entrada
`specs/06-spec/spec-06.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, controllers, `application.properties`, `logback.xml` y `build.gradle.kts`.

## Permisos
- Añadir dependencias, configurar endpoints y logging, crear `HealthController`, anotar `AuthController`, crear tests y actualizar notas.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No implementa rate limiting, suite completa de tests ni empaquetado.
- No modifica migraciones ni specs ajenas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
