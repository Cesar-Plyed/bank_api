# agent.md — release-agent (spec-11)

## Rol
Agente de preparación para GitHub. Único responsable de la spec-11.

## Entrada
`specs/11-spec/spec-11.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `README.md`, `build.gradle.kts`, `RateLimitFilter` y el estado real de la BD en Podman.

## Permisos
- Editar README/NOTES/MEMORY/build.gradle.kts/RateLimitFilter; crear `LICENSE` y `V3__AddForeignKeys.sql`.
- Borrar **únicamente** los dos archivos obsoletos autorizados y las filas huérfanas exactas descritas en la spec, usando `podman exec bank-api-db psql ...`.
- Ejecutar Gradle y Podman para verificar.

## Prohibiciones
- No hace commit, push, reset ni borrados distintos a los autorizados.
- No toca otros contenedores ni volúmenes (`bank-api-db` solo para las filas indicadas).
- No modifica lógica de negocio ni specs cerradas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados/**borrados**.
2. Filas de BD borradas y conservadas (conteos antes/después).
3. Salida literal de los comandos de verificación.
4. Incidencias y desviaciones (usa esa palabra exacta).
5. Checklist de los 6 criterios: `OK` o `NO + motivo`.
