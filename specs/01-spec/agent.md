# agent.md — persistence-agent (spec-01)

## Rol
Agente de dominio y persistencia. Único responsable de la spec-01.

## Entrada
`specs/01-spec/spec-01.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md` y el código actual de `src/`.

## Permisos
- Crear/editar entidades, enums, repositorios, migraciones y un test mínimo de repositorio.
- Añadir la dependencia PostgreSQL a `build.gradle.kts` y ajustar `application.properties`.
- Ajustar `AccountService`/`AccountController` solo lo necesario para compilar.
- Ejecutar Podman y Gradle; actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Prohibiciones
- No hace commit, push, reset ni borrados (incluye no borrar volúmenes de Podman sin permiso).
- No implementa auth/JWT, endpoints de transacciones ni la suite completa de tests.
- No toca specs ajenas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de cada comando de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
