# AGENTS.md — bank_api

## Estado real (el README describe el objetivo, no el código)
- `README.md` documenta JWT, PostgreSQL, transacciones y tests completos; el repo es un skeleton. Verifica `src/` antes de asumir que un endpoint existe.
- Entrada: `io.onstructive.micronaut.Application`; paquete base `io.onstructive.micronaut`.
- Stack verificado: Java 21 (`build.gradle.kts`), Micronaut 4.10.12 (`gradle.properties`), plugin Gradle 4.6.2, wrapper Gradle 8.14.3.
- Único controller: `AccountController` en `/accounts` (**sin** prefijo `/api`): list, create, deposit, withdraw, delete; IDs `UUID`.
- Seguridad desactivada: `micronaut.security.enabled=false`. No hay `User` ni JWT; las reglas `intercept-url-map` de Swagger están inactivas.
- Persistencia: H2 en memoria (`jdbc:h2:mem:devDb`) con `schema-generate=CREATE_DROP` → los datos se pierden al reiniciar. `AccountRepository` fija `Dialect.H2`. Flyway está habilitado pero **no hay migraciones V1/V2**.
- Swagger UI: `/swagger-ui.html` (recursos `classpath:META-INF/swagger`). `src/main/resources/openapi/bankaccount-0.0.yml` es un placeholder obsoleto (solo `/bankaccount`): no es fuente de verdad.

## Comandos exactos
- `./gradlew test` — suite actual: solo `BankaccountTest` (smoke test de arranque con `@MicronautTest`).
- `./gradlew test --tests "io.onstructive.micronaut.BankaccountTest"` — test puntual.
- `./gradlew run` / `./gradlew build` / `./gradlew classes`.
- Requiere JDK 21 y `JAVA_HOME`: esta máquina no tiene `java` en PATH, así que el wrapper falla hasta exportarlo. JDK instalado (spec-00): `/home/void/AI/apps/jdk-21` (Temurin 21.0.13.1+2) → `export JAVA_HOME=/home/void/AI/apps/jdk-21; export PATH="$JAVA_HOME/bin:$PATH"`. No hay PostgreSQL; sí hay Podman 6.1.4 para contenedores locales.

## Convenciones del repo
- Configuración real en `application.properties` (no YAML, pese al README), `logback.xml` y `openapi.properties`.
- `settings.gradle.kts`: `rootProject.name="bankaccount"` (el directorio se llama `bank_api`).
- Tests sin servicios externos hoy; `@MicronautTest` + `EmbeddedApplication`.
- Preguntar antes de borrar o sobrescribir archivos (convención global del equipo).

## Spec Driven Development (SDD) — reglas obligatorias
1. Toda implementación se divide en specs secuenciales `00, 01, …`; **un sub agente por spec**.
2. Cada spec vive en `specs/NN-spec/` y contiene solo `spec-NN.md` (objetivo, alcance, criterios de aceptación verificables, comandos, DoD) y `agent.md` (rol del agente, archivos permitidos, prohibiciones, formato de reporte).
3. Spec corta y atómica: un objetivo, un agente, un commit. Si crece, se parte en dos.
4. El sub agente **no commitea**: implementa, corre la prueba corta y reporta diff + incidencias.
5. El orquestador verifica el resultado y lo muestra al usuario; **no avanza sin aprobación explícita**. Si no aprueba, el mismo agente corrige.
6. Un commit por spec, tras la aprobación, con Conventional Commits: `<type>[<scope>][!]: <descripción>`. Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `perf`, `style`, `build`, `ci`, `chore`, `revert`. `!` o footer `BREAKING CHANGE:` para cambios rupturistas. `update` y `feature` no son tipos válidos → usar `refactor`/`docs` y `feat`. Ejemplo: `feat(auth): registro y login JWT (spec-02)`.
7. Tras cada spec: prueba corta en CPU (`./gradlew test` o el subconjunto), resultado visible e incidencias listadas; toda desviación se documenta en `NOTES.md`.
8. Prohibido: `push`, `reset`, borrados y tocar specs ajenas. Ante un bloqueo, detenerse y reportar.
9. Las specs se ejecutan en orden; las dependencias entre ellas son explícitas.

## Decisiones del dueño
- Contenedores con **Podman**, no Docker. Para Testcontainers: socket rootless de Podman con `DOCKER_HOST` / `TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE` y Ryuk desactivado.
- Primera implementación: solo los ítems `[x]` del roadmap + tests. Fuera: refresh tokens, auditoría, versionado, GraphQL, WebSockets y reportes.
- Estas reglas viven solo en este `AGENTS.md` del repo (no en `~/AGENTS.md`).
