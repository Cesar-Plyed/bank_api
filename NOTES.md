# NOTES.md — bank_api

Seguimiento de avance, cambios e incidencias. Fecha de hoy: **2026-11-25**.

## 2026-11-25 — spec-00: Bootstrap de entorno y andamiaje SDD

**Estado real del repo:** skeleton Micronaut 4.10.12 / Java 21. Único controller `AccountController` en `/accounts` (sin prefijo `/api`); seguridad desactivada (`micronaut.security.enabled=false`); persistencia H2 en memoria con `schema-generate=CREATE_DROP`; Flyway habilitado pero sin migraciones; Swagger UI en `/swagger-ui.html` con `src/main/resources/openapi/bankaccount-0.0.yml` como placeholder obsoleto. Único test: `BankaccountTest` (smoke test `@MicronautTest`).

**Acciones realizadas:**
- JDK 21 (Temurin `21.0.13.1+2`, LTS) descargado desde Adoptium a `/home/void/AI/downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz` (199 MB) y extraído en `/home/void/AI/apps/jdk-21`.
- `JAVA_HOME=/home/void/AI/apps/jdk-21` documentado en `AGENTS.md`; en cada sesión hay que exportarlo junto con `PATH="$JAVA_HOME/bin:$PATH"` (no hay `java` en PATH del sistema).
- Identidad git **local** del repo: `user.name=Cesar Eduardo Castañeda López`, `user.email=cesar.plyed@gmail.com` (tomada del usuario autenticado en `gh`, coincide con el owner del remote `Cesar-Plyed`).
- Creados `NOTES.md` y `MEMORY.md`.

**Decisiones del dueño (registradas también en `MEMORY.md`):**
- SDD: specs secuenciales `00, 01, …`, un sub agente por spec, un commit por spec tras aprobación, Conventional Commits. El sub agente no commitea; el orquestador verifica y no avanza sin aprobación explícita.
- Contenedores con **Podman**, no Docker. Para Testcontainers: socket rootless de Podman con `DOCKER_HOST` / `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` y Ryuk desactivado.
- Primera implementación: solo los ítems `[x]` del roadmap más tests. Fuera de alcance: refresh tokens, auditoría de transacciones, versionado de API, GraphQL, WebSockets y reportes financieros.
- Sin sudo: todo se instala bajo `~/AI/`. Nada de APIs en la nube: todo en `127.0.0.1`. RTX 4050 6 GB / i5-13420H / 16 GB RAM; un solo modelo grande cargado a la vez.

**Verificación (salida literal):**
- `java -version`: `openjdk version "21.0.13.1" 2026-11-17 LTS` / `OpenJDK Runtime Environment Temurin-21.0.13.1+2 (build 21.0.13.1+2-LTS)` / `OpenJDK 64-Bit Server VM Temurin-21.0.13.1+2 (build 21.0.13.1+2-LTS, mixed mode, sharing)`.
- `echo $JAVA_HOME`: `/home/void/AI/apps/jdk-21`; `readlink -f $(which java)`: `/home/void/AI/apps/jdk-21/bin/java`.
- `./gradlew --version`: Gradle 8.14.3, Launcher JVM 21.0.13.1 (Eclipse Adoptium 21.0.13.1+2-LTS), Daemon JVM `/home/void/AI/apps/jdk-21`. Descargó la distribución la primera vez.
- `./gradlew test`: `BUILD SUCCESSFUL in 1m 6s`, `5 actionable tasks: 5 executed`, exit 0. Genera `build/` (artefacto ignorado por git). Warning no bloqueante de SLF4J: "No SLF4J providers were found ... Defaulting to no-operation (NOP) logger implementation".
- `git config user.name`: `Cesar Eduardo Castañeda López`; `git config user.email`: `cesar.plyed@gmail.com`.
- `podman --version`: `podman version 6.1.4`; `podman info` funciona (runtime `runc`) con un warning no bloqueante: `"/" is not a shared mount, this could cause issues or missing mounts with rootless containers`.

**Incidencias y desviaciones:** ninguna bloqueante. Se documentan dos warnings no bloqueantes (SLF4J sin provider durante la compilación; Podman sobre `/` no compartido). El socket rootless de Podman no se probó con un contenedor real: solo se verificaron `podman --version` e `info`, que es lo que pedía la spec.

**No se hizo commit** (regla SDD): el orquestador debe verificar y pedir aprobación antes del commit, p. ej. `chore(build): bootstrap de entorno y andamiaje SDD (spec-00)`.
