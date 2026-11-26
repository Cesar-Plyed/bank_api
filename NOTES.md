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

## 2026-11-25 — spec-01: Dominio y persistencia (PostgreSQL + Flyway)

**Acciones realizadas:**
- Creados en `model/`: entidades `User`, `Account`, `Transaction` (records, IDs `UUID` asignados en código, **sin `@GeneratedValue`**, `BigDecimal` para dinero, `Instant` para fechas) y enums `AccountType`, `AccountStatus`, `TransactionType`, `TransactionStatus`.
- `Account` incorpora `ownerId`, `accountType`, `status`, `createdAt`; `Transaction` incorpora `fromAccountId`, `toAccountId`, `amount`, `type`, `status`, `timestamp`, `description`. `Account.create(...)` genera el UUID y usa `DEFAULT_OWNER_ID` porque el endpoint legacy `/accounts` aún no tiene campo owner.
- Repositorios con `Dialect.POSTGRES`: `UserRepository` (`findByUsername`, `existsByUsername`, `existsByEmail`), `AccountRepository` (`findByOwnerId`, `existsByAccountNumber`, más el `findByAccountNumber` previo) y `TransactionRepository` (`findByFromAccountIdOrToAccountId`).
- Migraciones `V1__CreateTables.sql` (users/accounts/transactions) y `V2__AddIndexes.sql` (índices de owner_id y de las columnas de transactions) en `src/main/resources/db/migration/`.
- `application.properties`: datasource PostgreSQL con valores por entorno y defaults (`BANK_DB_HOST`, `BANK_DB_PORT`, `BANK_DB_NAME`, `BANK_DB_USER`, `BANK_DB_PASSWORD`) y **`datasources.default.schema-generate=NONE`** → Flyway es el único dueño del esquema. `flyway.datasources.default.enabled=true`.
- `build.gradle.kts`: `runtimeOnly("org.postgresql:postgresql")` y `runtimeOnly("org.flywaydb:flyway-database-postgresql:10.22.0")` (Flyway 10 externalizó el soporte de PostgreSQL).
- Ajuste mínimo en `AccountService`: reconstruye `Account` con los nuevos campos en deposit/withdraw; `create` sigue usando `repo.save(...)` (en esta versión `save` siempre hace INSERT, verificado en el bytecode de `DefaultSaveEntityInterceptor`). `AccountController` no cambió.
- Test mínimo `RepositoryTest`: guarda y lee un `User` y una `Account` contra la BD real de Podman.

**Comandos Podman exactos (también en `AGENTS.md` y `MEMORY.md`):**
- Descargar imagen (una vez): `podman pull docker.io/library/postgres:15`
- Arrancar: `podman run -d --name bank-api-db -e POSTGRES_DB=bank_api -e POSTGRES_USER=bank_user -e POSTGRES_PASSWORD=bank_pass -p 127.0.0.1:5432:5432 docker.io/library/postgres:15`
- Parar / reiniciar: `podman stop bank-api-db` / `podman start bank-api-db`
- Estado: `podman ps`
- Verificar tablas: `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'`; índices: sustituir `\dt` por `\di`.
- **No se borró el contenedor ni ningún volumen.** No se tocaron los contenedores ajenos (`integrity-*`, `hq-*`, `ai-cuda`) que ya estaban parados.

**Verificación (salida literal):**
- Antes del primer arranque de la app: `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` → `Did not find any relations.`
- Después de `./gradlew test`: `podman ps` → contenedor `bank-api-db`, imagen `docker.io/library/postgres:15`, `Up`, `127.0.0.1:5432->5432/tcp`.
- `podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` → `accounts`, `flyway_schema_history`, `transactions`, `users` (4 rows).
- `podman exec bank-api-db psql -U bank_user -d bank_api -c '\di'` → `idx_accounts_owner_id`, `idx_transactions_from_account_id`, `idx_transactions_timestamp`, `idx_transactions_to_account_id`, más los índices primarios/únicos de V1.
- `SELECT version, description, success FROM flyway_schema_history ORDER BY version;` → `1 | CreateTables | t`, `2 | AddIndexes | t`.
- `./gradlew classes` → `BUILD SUCCESSFUL in 1s`. `./gradlew test` → `BUILD SUCCESSFUL in 3s`, `5 actionable tasks: 2 executed, 3 up-to-date`.

**Incidencias y desviaciones:**
1. El placeholder `${BANK_DB_URL:jdbc:postgresql://...}` se resolvió mal (Micronaut devolvió `5432/bank_api` y falló con `Invalid JDBC URL`). Se sustituyó por una URL compuesta con placeholders sin colon en el default: `jdbc:postgresql://${BANK_DB_HOST:127.0.0.1}:${BANK_DB_PORT:5432}/${BANK_DB_NAME:bank_api}`. Sigue siendo "valores por entorno con defaults", pero el nombre de variable es `BANK_DB_HOST/PORT/NAME`, no un único `BANK_DB_URL`.
2. Flyway 10.22.0 (traído por Micronaut Flyway 7.9.2) falló con `Unsupported Database: PostgreSQL 15.20` hasta añadir `org.flywaydb:flyway-database-postgresql:10.22.0`. Dependencia nueva no prevista literalmente en la spec, pero necesaria.
3. `CrudRepository` de Micronaut Data 4.14.3 no expone `insert(...)`; se usa `save(...)`, que internamente siempre genera un INSERT. Sin impacto funcional.
4. No se añadieron restricciones FK entre tablas (la spec solo pedía tablas + índices); `owner_id`, `from_account_id` y `to_account_id` son UUID sin FK. Anotado como decisión de diseño para no ampliar el alcance.
5. Se conservó la dependencia H2 en `build.gradle.kts` (ya no la usa el datasource) para minimizar cambios. Warning SLF4J sin providers sigue apareciendo, no bloqueante.
6. El contenedor `bank-api-db` queda **activo** a propósito: los tests lo necesitan. No se hizo commit (regla SDD).

**No se hizo commit** (regla SDD): el orquestador debe verificar y pedir aprobación antes del commit, p. ej. `feat(persistence): modelo de dominio y Flyway sobre PostgreSQL (spec-01)`.

## 2026-11-25 — spec-02: Autenticación JWT

**Acciones realizadas:**
- `build.gradle.kts`: añadidas `io.micronaut.security:micronaut-security-jwt` (resuelta 4.15.2, alineada con Micronaut 4.10.12) y `org.mindrot:jbcrypt:0.4`.
- `application.properties`: `micronaut.security.enabled=true`, secreto HS256 `micronaut.security.token.jwt.signatures.secret.generator.secret=${JWT_SECRET:dev-only-secret-change-me-please-32bytes}` (default **solo dev**, ≥32 bytes) y `micronaut.security.token.jwt.generator.access.expiration=3600`. Las reglas `intercept-url-map` de Swagger permanecen anonymous.
- DTOs `@Serdeable` con validación: `RegisterRequest`, `LoginRequest`, `LoginResponse`, `UserResponse` (este último nunca expone `passwordHash`).
- `security/PasswordEncoder.java`: BCrypt con 10 rondas (`BCrypt.gensalt(10)`); `matches` devuelve `false` para hashes corruptos en lugar de lanzar 500.
- `security/UserAuthenticationProvider.java`: implementa `AuthenticationProvider<HttpRequest<?>, String, String>`; valida contra `UserRepository.findByUsername` + BCrypt y devuelve `AuthenticationResponse.success(username, {userId, email})` o `failure()`.
- `security/JwtTokenService.java`: usa el `JwtTokenGenerator` de Micronaut con expiración explícita de 3600 s; el `sub` del JWT es el username.
- `controller/AuthController.java` en `/api/auth`: `POST /register` (201, rechaza username/email duplicados con 409) y `POST /login` (200 con `accessToken`, `tokenType=Bearer`, `expiresIn=3600`, `user`; 401 si las credenciales fallan). Ambos con `@PermitAll` y `@ExecuteOn(TaskExecutors.IO)`. No se tocó `AccountController`, `AccountService`, migraciones ni transacciones.
- `AuthControllerTest.java`: 4 tests HTTP contra la BD real de Podman (registro OK + hash BCrypt, duplicado 4xx, login OK con JWT de 3 segmentos y `sub`=username, login con password incorrecto 401). `@MicronautTest(transactional=false)` para que el cliente HTTP vea los datos confirmados.

**Verificación (salida literal):**
- `./gradlew test --tests "*Auth*Test" --console=plain` → `BUILD SUCCESSFUL in 3s`, `5 actionable tasks: 2 executed, 3 up-to-date`. XML: `<testsuite name="io.onstructive.micronaut.AuthControllerTest" tests="4" skipped="0" failures="0" errors="0" ...>`.
- `./gradlew test --console=plain` → `BUILD SUCCESSFUL in 3s`, `5 actionable tasks: 1 executed, 4 up-to-date`. XML: `AuthControllerTest tests="4"`, `BankaccountTest tests="1"`, `RepositoryTest tests="1"`, todos con `failures="0" errors="0"` (6 tests en total).

**Incidencias y desviaciones:**
1. Micronaut Security 4.15.2 **ya no incluye** la interfaz `io.micronaut.security.authentication.PasswordEncoder` (buscada en todos los jars del classpath, ausente). Se creó un bean propio `security.PasswordEncoder` sobre jBCrypt; no existe el módulo `micronaut-security-bcrypt` en Maven Central para esta versión (404).
2. `AuthenticationProvider` en Micronaut Security 4 es **síncrono** (`AuthenticationResponse authenticate(T, AuthenticationRequest<I,S>)`), no reactivo como en versiones 3.x. El controlador lo invoca directamente; se inyecta la implementación concreta para evitar ambigüedad de genéricos.
3. `JwtTokenGenerator.generateToken` en 4.x solo expone las sobrecargas `(Authentication, Integer)` y `(Map<String,Object>)`; se pasa 3600 explícitamente en vez de depender solo de la propiedad de configuración.
4. `@MicronautTest` tiene `transactional=true` por defecto: con peticiones HTTP el registro quedaría sin confirmar y el login no lo vería. Se fijó `transactional=false` en `AuthControllerTest`; las filas de prueba usan suffixes UUID y quedan en la BD (convención ya aceptada en spec-01).
5. `GlobalExceptionHandler` preexistente (skeleton, spec-05) no se modificó: el controlador devuelve `HttpResponse` con estados explícitos (201/409/401/200) para no depender del manejo global de errores.
6. El primer intento de compilación del test falló porque `HttpClient.exchange` es reactivo (`Publisher<HttpResponse<O>>`); se corrigió usando `client.toBlocking().exchange(...)`. Sin impacto funcional.
7. Con seguridad activa, los endpoints legacy `/accounts` ahora exigen autenticación (no se migraron a `/api/accounts` ni se les dio ownership: eso es spec-03). Ningún test existente los invoca.
8. Contenedor `bank-api-db` dejado activo; no se borró nada. No se hizo commit (regla SDD).

**No se hizo commit** (regla SDD): el orquestador debe verificar y pedir aprobación antes del commit, p. ej. `feat(auth): registro y login JWT (spec-02)`.

## 2026-11-25 — spec-03: Cuentas con dueño bajo `/api/accounts`

**Acciones realizadas:**
- `AccountController` migrado de `/accounts` a `/api/accounts` con `@Secured(SecurityRule.IS_AUTHENTICATED)`: `GET` (list), `POST` (create), `GET /{id}`, `DELETE /{id}`. Eliminados `deposit`/`withdraw` del controller (quedan para `/api/transactions`, spec-04). El controller lee el `ownerId` del claim `userId` del JWT inyectando `Authentication`; nunca del body. Ante un id ajeno o inexistente responde 404; borrar con saldo responde 400; sin token responde 401 (controlado por el filtro de seguridad).
- `CreateAccountRequest` ampliado con `@NotNull AccountType accountType` y `@PositiveOrZero BigDecimal initialBalance` (omitido ⇒ 0). `Account.create(ownerId, accountNumber, currency, accountType, initialBalance)` recibe el dueño real; **`DEFAULT_OWNER_ID` eliminado**.
- `AccountService`: `create(req, ownerId)`, `findAllByOwner(ownerId)`, `findByIdForOwner(id, ownerId)`, `delete(id, ownerId)`; conserva `deposit`/`withdraw` sin cambios funcionales. No se tocó `AccountRepository` (ya tenía `findByOwnerId`), ni auth, ni migraciones, ni `application.properties`.
- `AccountControllerTest.java`: 6 tests HTTP con JWT real obtenido vía registro+login: dueño = claim `userId`, balance por defecto 0, listado solo propio, get de cuenta ajena/inexistente → 404, borrar con saldo → 4xx y borrar propia con saldo 0 → 204, sin token → 401, ruta legacy `/accounts` fuera de servicio. `@MicronautTest(transactional=false)`; sufijos UUID para no colisionar con filas anteriores.

**Verificación (salida literal):**
- `./gradlew test --tests "*Account*Test" --console=plain`:
  ```
  > Task :compileJava UP-TO-DATE
  > Task :processResources UP-TO-DATE
  > Task :classes UP-TO-DATE
  > Task :compileTestJava
  > Task :inspectRuntimeClasspath UP-TO-DATE
  > Task :processTestResources NO-SOURCE
  > Task :testClasses UP-TO-DATE
  > Task :test

  BUILD SUCCESSFUL in 4s
  5 actionable tasks: 2 executed, 3 up-to-date
  ```
- `./gradlew test --console=plain`:
  ```
  > Task :compileJava UP-TO-DATE
  > Task :processResources UP-TO-DATE
  > Task :classes UP-TO-DATE
  > Task :compileTestJava UP-TO-DATE
  > Task :inspectRuntimeClasspath UP-TO-DATE
  > Task :processTestResources NO-SOURCE
  > Task :testClasses UP-TO-DATE
  > Task :test

  BUILD SUCCESSFUL in 5s
  5 actionable tasks: 1 executed, 4 up-to-date
  ```
- XML de resultados: `AccountControllerTest: tests=6 skipped=0 failures=0 errors=0`; `AuthControllerTest: tests=4 skipped=0 failures=0 errors=0`; `BankaccountTest: tests=1 skipped=0 failures=0 errors=0`; `RepositoryTest: tests=1 skipped=0 failures=0 errors=0` (**12 tests, 0 failures/errors**).

**Incidencias y desviaciones:**
1. Micronaut Security 4 **rechaza con 403 las rutas sin una regla explícita de autorización**, aunque el JWT sea válido (`SecuredAnnotationRule` devuelve `UNKNOWN` y `SecurityFilter` cae en `AuthorizationException`). Se añadió `@Secured(SecurityRule.IS_AUTHENTICATED)` en `AccountController` (archivo permitido); sin él, los 5 tests autenticados fallaban con 403. No se tocó `application.properties` (fuera de permisos de la spec).
2. La ruta legacy `/accounts` con token devuelve **403, no 404**: el filtro de seguridad no encuentra regla para una ruta inexistente y responde 403 antes del router. El test verifica que es 4xx (fuera de servicio) en vez de exigir 404 literal.
3. `initialBalance` se declaró nullable en el DTO y el valor por defecto (0) se aplica en `AccountService`; `@PositiveOrZero` ignora `null`, así que un body sin el campo es válido y crea saldo 0.
4. `@ExecuteOn(TaskExecutors.IO)` se añadió al controller para no ejecutar JDBC en el event loop (misma convención que `AuthController`). Pequeña mejora no pedida literalmente por la spec.
5. `GlobalExceptionHandler` (spec-05) sigue intacto: el controller traduce sus propias excepciones a 404/400 sin manejo global.
6. Contenedor `bank-api-db` dejado activo; no se borró nada. Las filas de prueba quedan en la BD (convención ya aceptada). No se hizo commit (regla SDD): sugerencia `feat(accounts): CRUD bajo /api/accounts con dueño desde JWT (spec-03)`.
