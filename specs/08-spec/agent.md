# agent.md — testing-agent (spec-08)

## Rol
Agente de tests y cobertura. Único responsable de la spec-08.

## Entrada
`specs/08-spec/spec-08.md`, `AGENTS.md`, `NOTES.md`, `MEMORY.md`, `build.gradle.kts`, `application-test.yml`, servicios, controllers y tests existentes.

## Permisos
- Añadir el plugin JaCoCo y sus tareas, crear tests de servicio e integración y actualizar notas.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No baja el umbral de cobertura para cumplir; añade tests.
- No modifica migraciones, seguridad ni specs ajenas. Ante un bloqueo, se detiene y reporta.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de los comandos de verificación y porcentaje de cobertura.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 5 criterios de aceptación: `OK` o `NO + motivo`.
