# Spec-09 — Empaquetado con Podman

## Objetivo
Generar el fat JAR y una imagen multi-stage, y verificarla arrancándola con Podman.

## Alcance
- `Dockerfile` multi-stage: etapa de build con JDK 21 que ejecuta `./gradlew --no-daemon shadowJar`; etapa final con JRE 21, usuario no root, `EXPOSE 8080` y el JAR copiado.
- `.dockerignore` que excluya `.git`, `.gradle`, `build/`, `.idea/` y similares para no ensuciar el contexto.
- Configurar `micronaut.server.port=${SERVER_PORT:8080}` para poder mapear otro puerto en el smoke test.
- Verificar `./gradlew shadowJar` y listar el JAR resultante en `build/libs/`.
- Construir con `podman build -t bank-api:spec09 .` y smoke test: arrancar contenedor temporal con `--rm` (por ejemplo `--network host` para alcanzar `bank-api-db` en `127.0.0.1:5432`), hacer `GET http://127.0.0.1:8080/health` → 200 `{"status":"UP"}` y parar el contenedor. No tocar `bank-api-db` ni contenedores/volúmenes ajenos.
- Documentar comandos de build/run, el comando opcional `./gradlew nativeImage` (GraalVM no instalado) y las desviaciones en `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Fuera de alcance
Cierre documental final (spec-10), commits, cambios de lógica de negocio.

## Criterios de aceptación
1. `./gradlew shadowJar` produce un JAR ejecutable en `build/libs/`.
2. `podman build -t bank-api:spec09 .` termina correctamente.
3. El contenedor responde `/health` con 200 y `{"status":"UP"}`, y se detiene limpiamente.
4. Los 55 tests y `jacocoTestCoverageVerification` siguen pasando.
5. Notas actualizadas con build, run y desviaciones.

## Comandos de verificación
`./gradlew shadowJar` · `podman build -t bank-api:spec09 .` · smoke test `/health` · `./gradlew test jacocoTestCoverageVerification`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
