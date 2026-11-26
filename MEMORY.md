# MEMORY.md — bank_api

Decisiones, contexto y lecciones para el agente que retome el trabajo. Actualizado: **2026-11-25**.

## Estado actual
- Repo skeleton Micronaut 4.10.12 / Java 21 / Gradle wrapper 8.14.3. `rootProject.name="bankaccount"` (carpeta `bank_api`). Entrada `io.onstructive.micronaut.Application`.
- Controllers: `AccountController` en `/api/accounts` (spec-03: `GET`, `POST`, `GET /{id}`, `DELETE /{id}`; dueño desde el claim `userId` del JWT; sin deposit/withdraw) y `AuthController` en `/api/auth` (spec-02: `POST /register`, `POST /login`, `@PermitAll`). La ruta legacy `/accounts` ya no existe.
- Seguridad **activada** (spec-02, propiedad en spec-03): `micronaut.security.enabled=true`, JWT HS256 con `JWT_SECRET` (default dev ≥32 bytes, **definir siempre en producción**), expiración 3600 s. `/api/auth/**` público; `/api/accounts` con `@Secured(SecurityRule.IS_AUTHENTICATED)` y filtro por `userId`. Swagger sigue anonymous. Contraseñas BCrypt 10 rondas.
- Persistencia (spec-01): PostgreSQL 15 en Podman rootless (`127.0.0.1:5432`, db `bank_api`, usuario `bank_user`, password `bank_pass`). Tres repositorios con `Dialect.POSTGRES`. **`datasources.default.schema-generate=NONE`**: Flyway es dueño del esquema con `V1__CreateTables.sql` y `V2__AddIndexes.sql`.
- Configuración real: `application.properties`, `logback.xml`, `openapi.properties`. `src/main/resources/openapi/bankaccount-0.0.yml` es placeholder obsoleto; no es fuente de verdad.
- Tests: `BankaccountTest` (smoke `@MicronautTest`) + `RepositoryTest` (spec-01) + `AuthControllerTest` (spec-02, 4 tests HTTP) + `AccountControllerTest` (spec-03, 6 tests HTTP con JWT real). `./gradlew test` pasa con la BD de Podman activa (2026-11-25): 12 tests, 0 failures/errors.
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

## Spec-02 — autenticación JWT (2026-11-25)
- Dependencias: `io.micronaut.security:micronaut-security-jwt` (4.15.2) y `org.mindrot:jbcrypt:0.4`. No existe `micronaut-security-bcrypt` en Maven Central para Micronaut Security 4.
- `JWT_SECRET` se lee desde `application.properties` con default de desarrollo: `micronaut.security.token.jwt.signatures.secret.generator.secret=${JWT_SECRET:dev-only-secret-change-me-please-32bytes}`. HS256 exige ≥32 bytes; en producción se debe exportar `JWT_SECRET`. Expiración configurada en 3600 s (`micronaut.security.token.jwt.generator.access.expiration`), además se pasa 3600 al generador.
- Micronaut Security 4 eliminó `io.micronaut.security.authentication.PasswordEncoder`: se define un bean propio con `BCrypt.gensalt(10)` / `BCrypt.checkpw`. Prefijo de hash `$2a$`/`$2b$`.
- `AuthenticationProvider` en 4.x es **síncrono**: `AuthenticationResponse authenticate(HttpRequest<?>, AuthenticationRequest<String,String>)`. Se inyecta la implementación concreta (`UserAuthenticationProvider`) porque los genéricos dificultan la inyección por interfaz.
- `JwtTokenGenerator.generateToken(Authentication, Integer)` (4.x) genera `sub` = `authentication.getName()` = username, `iat`, `exp` = ahora + 3600 s y lo firma con el secreto configurado.
- `@PermitAll` (JSR-250, `jakarta.annotation.security.PermitAll`) funciona con `micronaut-security-annotations` para hacer públicos ambos endpoints. Sin token, las peticiones a `/api/auth/**` pasan; el resto está autenticado por defecto.
- Lección: `@MicronautTest` es `transactional=true` por defecto; con `HttpClient` hay que usar `@MicronautTest(transactional=false)` o el registro no se ve desde el login (distinta conexión JDBC).
- Lección: `HttpClient.exchange` es reactivo; en tests usar `client.toBlocking().exchange(...)`.
- `GlobalExceptionHandler` del skeleton permanece intacto (spec-05): el `AuthController` devuelve `HttpResponse` con estados explícitos (201/409/200/401) en lugar de lanzar excepciones.
- Sin commit: queda pendiente `feat(auth): registro y login JWT (spec-02)`.

## Spec-03 — cuentas con dueño bajo `/api/accounts` (2026-11-25)
- `AccountController` en `/api/accounts` con `@Secured(SecurityRule.IS_AUTHENTICATED)` y `@ExecuteOn(TaskExecutors.IO)`. Inyecta `Authentication` como parámetro (lo resuelve `AuthenticationArgumentBinder`) y lee `getAttributes().get("userId")`. Sin `@Secured`, Micronaut Security devuelve **403 incluso con JWT válido**: `SecuredAnnotationRule` reporta `UNKNOWN` y `SecurityFilter` lanza `AuthorizationException`. `@PermitAll` no sirve porque haría público el endpoint.
- Ownership: `AccountService.findByIdForOwner(id, ownerId)` filtra por `ownerId`; inexistente y ajeno se tratan igual (404 desde el controller). `delete` exige además `balance == 0` (400 si no). `deposit`/`withdraw` se conservan en el servicio para spec-04, pero ya no hay rutas legacy.
- `Account.create(ownerId, accountNumber, currency, accountType, initialBalance)`; `DEFAULT_OWNER_ID` eliminado. `CreateAccountRequest` añade `accountType` (`@NotNull`) e `initialBalance` (`@PositiveOrZero`, nullable ⇒ 0 en el servicio).
- `Authentication` en el JWT: `sub` = username, atributos `userId` (UUID en texto) y `email`. `getRoles()` de `AbstractSecurityRule` añade los pseudoroles `isAnonymous()`/`isAuthenticated()`, por lo que `SecurityRule.IS_AUTHENTICATED` funciona sin roles reales.
- Lección: `/accounts` (sin ruta) con token devuelve 403, no 404, por el filtro de seguridad; el test de "ruta desaparecida" comprueba 4xx.
- Lección: `HttpClient.exchange(HttpRequest, Argument.listOf(Account.class))` para obtener `HttpResponse<List<Account>>` tipado (con `List.class` solo se obtiene `HttpResponse<List>` sin genérico).
- Sin commit: queda pendiente `feat(accounts): CRUD bajo /api/accounts con dueño desde JWT (spec-03)`.
