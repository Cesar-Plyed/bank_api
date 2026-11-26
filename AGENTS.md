# AGENTS.md — bank_api

## Estado real (el README describe el objetivo, no el código)
- `README.md` documenta JWT, PostgreSQL, transacciones y tests completos; el repo es un skeleton. Verifica `src/` antes de asumir que un endpoint existe.
- Entrada: `io.onstructive.micronaut.Application`; paquete base `io.onstructive.micronaut`.
- Stack verificado: Java 21 (`build.gradle.kts`), Micronaut 4.10.12 (`gradle.properties`), plugin Gradle 4.6.2, wrapper Gradle 8.14.3.
- Controllers: `AccountController` en `/api/accounts` (spec-03): `GET` (list), `POST` (create), `GET /{id}`, `DELETE /{id}`; IDs `UUID`; `ownerId` tomado del claim `userId` del JWT; `deposit`/`withdraw` retirados del controller (se reimplementarán en `/api/transactions`, spec-04). La ruta legacy `/accounts` ya no existe. `AuthController` en `/api/auth` (spec-02): `POST /register`, `POST /login`, ambos `@PermitAll`.
- Seguridad **activada** (spec-02, propiedad en spec-03): `micronaut.security.enabled=true` + `micronaut-security-jwt`, HS256 con `JWT_SECRET` (default solamente de desarrollo en `application.properties`, ≥32 bytes). `/api/auth/**` es público (`@PermitAll`); `/api/accounts` lleva `@Secured(SecurityRule.IS_AUTHENTICATED)` y filtra siempre por el `userId` del JWT (sin este annotation Micronaut Security responde 403 incluso con token válido). Las reglas `intercept-url-map` de Swagger siguen anonymous. Hash de contraseñas BCrypt 10 rondas (`$2a$`/`$2b$`), nunca texto plano.
- Persistencia (spec-01): PostgreSQL 15 en Podman rootless en `127.0.0.1:5432` (db `bank_api`, usuario `bank_user`, password `bank_pass`). Los tres repositorios fijan `Dialect.POSTGRES`. **`datasources.default.schema-generate=NONE`**: Flyway es el único dueño del esquema (`V1__CreateTables.sql`, `V2__AddIndexes.sql` en `src/main/resources/db/migration/`).
- Swagger UI: `/swagger-ui.html` (recursos `classpath:META-INF/swagger`). `src/main/resources/openapi/bankaccount-0.0.yml` es un placeholder obsoleto (solo `/bankaccount`): no es fuente de verdad.

## Comandos exactos
- `./gradlew test` — suite actual (12 tests): `BankaccountTest` (smoke de arranque) + `RepositoryTest` (spec-01, guarda/lee `User` y `Account`) + `AuthControllerTest` (spec-02, 4 tests HTTP de registro/login) + `AccountControllerTest` (spec-03, 6 tests HTTP de cuentas con JWT real).
- `./gradlew test --tests "io.onstructive.micronaut.BankaccountTest"` — test puntual.
- `./gradlew test --tests "*Auth*Test"` — solo los tests de autenticación (spec-02).
- `./gradlew test --tests "*Account*Test"` — solo los tests de cuentas con JWT real (spec-03).
- `./gradlew run` / `./gradlew build` / `./gradlew classes`.
- Requiere JDK 21 y `JAVA_HOME`: esta máquina no tiene `java` en PATH, así que el wrapper falla hasta exportarlo. JDK instalado (spec-00): `/home/void/AI/apps/jdk-21` (Temurin 21.0.13.1+2) → `export JAVA_HOME=/home/void/AI/apps/jdk-21; export PATH="$JAVA_HOME/bin:$PATH"`. No hay PostgreSQL en el host; sí hay Podman 6.1.4 para contenedores locales.
- Base de datos (spec-01), **Podman, no Docker**. Arrancar:
  `podman pull docker.io/library/postgres:15` (una sola vez) y
  `podman run -d --name bank-api-db -e POSTGRES_DB=bank_api -e POSTGRES_USER=bank_user -e POSTGRES_PASSWORD=bank_pass -p 127.0.0.1:5432:5432 docker.io/library/postgres:15`.
- Parar: `podman stop bank-api-db`. Arrancar de nuevo: `podman start bank-api-db`. Estado: `podman ps`. **No borrar el contenedor ni su volumen sin permiso explícito.**
- Verificar esquema: `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` (y `-c '\di'` para índices). Las apps/tests necesitan el contenedor activo; `./gradlew test` falla si la BD no está arriba.
- Datasource por entorno (defaults incluidos): `BANK_DB_HOST=127.0.0.1`, `BANK_DB_PORT=5432`, `BANK_DB_NAME=bank_api`, `BANK_DB_USER=bank_user`, `BANK_DB_PASSWORD=bank_pass`. El esquema **no** lo genera Micronaut: `datasources.default.schema-generate=NONE` y Flyway aplica V1/V2 en el primer arranque.
- JWT (spec-02): `JWT_SECRET=${JWT_SECRET:dev-only-secret-change-me-please-32bytes}`; **en producción hay que definir `JWT_SECRET`** con al menos 32 bytes (256 bits) para HS256. El default existe solo para desarrollo/tests. Expiración 3600 s.

## Convenciones del repo
- Configuración real en `application.properties` (no YAML, pese al README), `logback.xml` y `openapi.properties`.
- `settings.gradle.kts`: `rootProject.name="bankaccount"` (el directorio se llama `bank_api`).
- Tests con `@MicronautTest` + `EmbeddedApplication`: `BankaccountTest` (smoke) y `RepositoryTest` (spec-01). Los tests HTTP (`AuthControllerTest`, `AccountControllerTest`) usan `@MicronautTest(transactional=false)` para que el cliente vea los datos confirmados. Requieren la BD de Podman activa.
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
