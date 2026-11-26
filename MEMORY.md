# MEMORY.md — bank_api

Decisiones, contexto y lecciones para el agente que retome el trabajo. Actualizado: **2026-11-25**.

## Estado actual
- Repo skeleton Micronaut 4.10.12 / Java 21 / Gradle wrapper 8.14.3. `rootProject.name="bankaccount"` (carpeta `bank_api`). Entrada `io.onstructive.micronaut.Application`.
- Único controller: `AccountController` en `/accounts` (sin `/api`): list, create, deposit, withdraw, delete; IDs `UUID`.
- Seguridad desactivada: no hay `User` ni JWT todavía; las reglas `intercept-url-map` de Swagger están inactivas.
- Persistencia: H2 en memoria (`jdbc:h2:mem:devDb`), `schema-generate=CREATE_DROP` (los datos se pierden al reiniciar). `AccountRepository` fija `Dialect.H2`. Flyway habilitado pero sin migraciones V1/V2.
- Configuración real: `application.properties`, `logback.xml`, `openapi.properties`. `src/main/resources/openapi/bankaccount-0.0.yml` es placeholder obsoleto; no es fuente de verdad.
- Único test hoy: `BankaccountTest` (smoke `@MicronautTest`). `./gradlew test` pasa (2026-11-25).
- `README.md` describe el objetivo, no el código implementado. El estado real prevalece: verifica `src/` antes de asumir endpoints.

## Entorno (spec-00)
- JDK: Temurin 21.0.13.1+2 en `/home/void/AI/apps/jdk-21`. Tarball en `/home/void/AI/downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz`.
- **No hay `java` en PATH.** Antes de cualquier comando Gradle: `export JAVA_HOME=/home/void/AI/apps/jdk-21; export PATH="$JAVA_HOME/bin:$PATH"`.
- Podman 6.1.4 disponible; no hay PostgreSQL ni Docker. Sin sudo: todo bajo `~/AI/`.
- Identidad git local: `Cesar Eduardo Castañeda López <cesar.plyed@gmail.com>`.
- Hardware: RTX 4050 Laptop 6 GB, i5-13420H, 16 GB RAM; un solo modelo grande cargado a la vez; sin APIs de nube (todo en `127.0.0.1`).

## Decisiones del dueño
- **SDD obligatorio:** specs `00, 01, …`, un sub agente por spec, spec corta y atómica (un objetivo, un agente, un commit). El sub agente no commitea: implementa, corre prueba corta y reporta. El orquestador verifica, muestra al usuario y **no avanza sin aprobación explícita**. Un commit por spec aprobada con Conventional Commits (`feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `style`, `build`, `ci`, `chore`, `revert`; `update`/`feature` no válidos). Prohibido push, reset, borrados y tocar specs ajenas. Ante bloqueo: detenerse y reportar. Tras cada spec: prueba corta en CPU, resultado visible, incidencias en `NOTES.md`.
- **Podman, no Docker.** Testcontainers con socket rootless: `DOCKER_HOST` / `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`, Ryuk desactivado.
- **Alcance de la primera implementación:** solo ítems `[x]` del roadmap (JWT básico, CRUD de cuentas, transacciones, Swagger UI, Flyway migrations) + tests. Fuera: refresh tokens, auditoría, versionado, GraphQL, WebSockets, reportes financieros.
- Convención global: preguntar antes de borrar o sobrescribir archivos. `~/` solo contiene carpetas estándar; excepción `~/AI/` (raíz de todo el trabajo con IA).

## Lecciones
- El wrapper de Gradle falla sin `JAVA_HOME`; se resuelve exportando la ruta del JDK, no instalando nada vía sudo.
- `./gradlew --version` descarga la distribución 8.14.3 la primera vez; `./gradlew test` compila y genera OpenAPI en `build/classes/java/main/META-INF/swagger`.
- `./gradlew test` emite un warning SLF4J (sin providers) que no falla el build.
