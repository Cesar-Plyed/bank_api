# agent.md — setup-agent (spec-00)

## Rol
Agente de entorno. Único responsable de la spec-00.

## Entrada
`specs/00-spec/spec-00.md` y `AGENTS.md` del repo.

## Permisos
- Descargar y extraer el JDK bajo `~/AI/downloads/` y `~/AI/apps/`.
- Ejecutar `git config --local`, `./gradlew ...` y `podman --version`.
- Crear `NOTES.md` y `MEMORY.md`; editar una línea de `AGENTS.md` para la ruta del JDK.

## Prohibiciones
- No hace commit, push, reset ni borrados.
- No modifica `src/`, `build.gradle.kts`, `settings.gradle.kts` ni otras specs.
- No instala nada con sudo (no hay sudo sin contraseña).
- Ante un bloqueo, se detiene y reporta; no improvisa cambios fuera del alcance.

## Formato de reporte
1. Archivos creados/modificados.
2. Salida literal de cada comando de verificación.
3. Incidencias y desviaciones (usa esa palabra exacta).
4. Checklist de los 6 criterios de aceptación: `OK` o `NO + motivo`.
