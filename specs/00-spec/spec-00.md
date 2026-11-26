# Spec-00 — Bootstrap de entorno y andamiaje SDD

## Objetivo
Dejar el repo ejecutable y con seguimiento antes de tocar código de negocio.

## Alcance
- Instalar JDK 21 sin root: descargar Temurin desde Adoptium a `~/AI/downloads/`, extraer en `~/AI/apps/jdk-21` y documentar `JAVA_HOME`.
- Configurar identidad git **local** del repo (`user.name`, `user.email`).
- Crear `NOTES.md` y `MEMORY.md` con fecha de hoy, estado actual y decisiones del dueño (SDD, Podman, alcance `[x]` del roadmap + tests).
- Añadir a `AGENTS.md` la ruta exacta del JDK instalado.
- Verificar `java -version`, `./gradlew --version`, `./gradlew test`, `podman --version` y `git config`.

## Fuera de alcance
- Código de negocio (specs 01+), commits/push, borrados, instalación con sudo.

## Criterios de aceptación
1. `java -version` reporta 21.x y `JAVA_HOME` apunta al JDK instalado.
2. `./gradlew --version` funciona; `./gradlew test` pasa o el fallo exacto queda registrado en `NOTES.md`.
3. `git config user.name` y `git config user.email` devuelven valores no vacíos.
4. Existen `NOTES.md` y `MEMORY.md` con fecha, estado "skeleton" y decisiones del dueño.
5. `AGENTS.md` incluye la ruta local del JDK.
6. `podman --version` funciona; si el socket rootless no está activo, se documenta en `NOTES.md`.

## Comandos de verificación
`java -version` · `./gradlew --version` · `./gradlew test` · `git config user.name` · `git config user.email` · `podman --version`

## Definition of Done
Reportar archivos creados/modificados, salida literal de cada comando, incidencias/desviaciones y el checklist de criterios. **No hacer commit.**
