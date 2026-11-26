# MEMORY.md — bank_api

Decisiones, contexto y lecciones para el agente que retome el trabajo. Actualizado: **2026-11-25**.

## Estado actual
- Repo skeleton Micronaut 4.10.12 / Java 21 / Gradle wrapper 8.14.3. `rootProject.name="bankaccount"` (carpeta `bank_api`). Entrada `io.onstructive.micronaut.Application`.
- Único controller: `AccountController` en `/accounts` (sin `/api`): list, create, deposit, withdraw, delete; IDs `UUID`.
- Seguridad desactivada: no hay `User` ni JWT todavía; las reglas `intercept-url-map` de Swagger están inactivas.
- Persistencia (spec-01): PostgreSQL 15 en Podman rootless (`127.0.0.1:5432`, db `bank_api`, usuario `bank_user`, password `bank_pass`). Tres repositorios con `Dialect.POSTGRES`. **`datasources.default.schema-generate=NONE`**: Flyway es dueño del esquema con `V1__CreateTables.sql` y `V2__AddIndexes.sql`.
- Configuración real: `application.properties`, `logback.xml`, `openapi.properties`. `src/main/resources/openapi/bankaccount-0.0.yml` es placeholder obsoleto; no es fuente de verdad.
- Tests: `BankaccountTest` (smoke `@MicronautTest`) + `RepositoryTest` (spec-01). `./gradlew test` pasa con la BD de Podman activa (2026-11-25).
- `README.md` describe el objetivo, no el código implementado. El estado real prevalece: verifica `src/` antes de asumir endpoints.

## Entorno (spec-00)
- JDK: Temurin 21.0.13.1+2 en `/home/void/AI/apps/jdk-21`. Tarball en `/home/void/AI/downloads/OpenJDK21U-jdk_x64_linux_hotspot_21.0.13.1_2.tar.gz`.
- **No hay `java` en PATH.** Antes de cualquier comando Gradle: `export JAVA_HOME=/home/void/AI/apps/jdk-21; export PATH="$JAVA_HOME/bin:$PATH"`.
- Podman 6.1.4 disponible; no hay PostgreSQL en el host ni Docker. Sin sudo: todo bajo `~/AI/`.
- **Comandos de la BD (spec-01):** `podman pull docker.io/library/postgres:15`; `podman run -d --name bank-api-db -e POSTGRES_DB=bank_api -e POSTGRES_USER=bank_user -e POSTGRES_PASSWORD=bank_pass -p 127.0.0.1:5432:5432 docker.io/library/postgres:15`; `podman stop bank-api-db` / `podman start bank-api-db`; `podman ps`; `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'`. Prohibido borrar el contenedor/volumen sin permiso. Contenedor dejado **activo** para los tests.
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

## Spec-01 — dominio y persistencia (2026-11-25)
- Modelo: `User`, `Account`, `Transaction` + 4 enums en `model/`. IDs `UUID` asignados en código (sin `@GeneratedValue`); `Account.create(...)` rellena `DEFAULT_OWNER_ID` porque el endpoint legacy no tiene owner.
- `AccountService` reconstruye `Account` con los nuevos campos en deposit/withdraw. `save()` hace INSERT en Micronaut Data 4.14.3 (`CrudRepository` no expone `insert`). No se cambió `AccountController`.
- Datasource por entorno: `BANK_DB_HOST`, `BANK_DB_PORT`, `BANK_DB_NAME`, `BANK_DB_USER`, `BANK_DB_PASSWORD` (con defaults). **`schema-generate=NONE`**; Flyway aplica V1/V2 en el primer arranque (verificado: antes había `Did not find any relations`, después aparecieron las 3 tablas + `flyway_schema_history` y los índices).
- Lección: no usar `${VAR:jdbc:postgresql://host:5432/db}` en `application.properties` (Micronaut resolvió solo `5432/bank_api`); compose la URL con placeholders sin colon en el default.
- Lección: Flyway 10.22.0 necesita `org.flywaydb:flyway-database-postgresql:10.22.0` para PostgreSQL 15, si no falla con `Unsupported Database: PostgreSQL 15.20`.
- Sin FK entre tablas (fuera de alcance de la spec); H2 se conserva en el build pero ya no se usa.
- Sin commit: queda pendiente la aprobación del orquestador (`feat(persistence): modelo de dominio y Flyway sobre PostgreSQL (spec-01)`).
