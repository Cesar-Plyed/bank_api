# MEMORY.md — bank_api

Decisiones, contexto y lecciones para el agente que retome el trabajo. Actualizado: **2026-11-25**.

## Estado actual
- Repo skeleton Micronaut 4.10.12 / Java 21 / Gradle wrapper 8.14.3. `rootProject.name="bankaccount"` (carpeta `bank_api`). Entrada `io.onstructive.micronaut.Application`.
- Controllers: `AccountController` en `/api/accounts` (spec-03: `GET`, `POST`, `GET /{id}`, `DELETE /{id}`; dueño desde el claim `userId` del JWT; sin deposit/withdraw), `AuthController` en `/api/auth` (spec-02: `POST /register`, `POST /login`, `@PermitAll`) y `TransactionController` (spec-04, base `/api`: `POST /api/transactions/{deposit,withdraw,transfer}` y `GET /api/accounts/{accountId}/transactions`; `@Secured` + `@ExecuteOn(IO)`). La ruta legacy `/accounts` ya no existe.
- Seguridad **activada** (spec-02, propiedad en spec-03): `micronaut.security.enabled=true`, JWT HS256 con `JWT_SECRET` (default dev ≥32 bytes, **definir siempre en producción**), expiración 3600 s. `/api/auth/**` público; `/api/accounts` con `@Secured(SecurityRule.IS_AUTHENTICATED)` y filtro por `userId`. Swagger sigue anonymous. Contraseñas BCrypt 10 rondas.
- Persistencia (spec-01): PostgreSQL 15 en Podman rootless (`127.0.0.1:5432`, db `bank_api`, usuario `bank_user`, password `bank_pass`). Tres repositorios con `Dialect.POSTGRES`. **`datasources.default.schema-generate=NONE`**: Flyway es dueño del esquema con `V1__CreateTables.sql` y `V2__AddIndexes.sql`.
- Configuración real: `application.properties`, `logback.xml`, `openapi.properties`. `src/main/resources/openapi/bankaccount-0.0.yml` es placeholder obsoleto; no es fuente de verdad.
- Tests: `BankaccountTest` (smoke `@MicronautTest`) + `RepositoryTest` (spec-01) + `AuthControllerTest` (spec-02, 4) + `AccountControllerTest` (spec-03, 6) + `TransactionControllerTest` (spec-04, 7) + `ErrorHandlingTest` (spec-05, 8) + `HealthControllerTest` (spec-06, 5) + `RateLimitTest` (spec-07, 4) + `RateLimitDisabledTest` (spec-07, 1) + `AccountServiceTest` (spec-08, 8) + `TransactionServiceTest` (spec-08, 8) + `BankApiIntegrationTest` (spec-08, 2 E2E). `./gradlew test` pasa con la BD de Podman activa (2026-11-25): **55 tests, 0 failures/errors en 12 clases**. Cobertura JaCoCo: **90.24% líneas (305/338)**, verificación ≥70% superada sin bajar el umbral.
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

## Spec-04 — transacciones ACID (2026-11-25)
- `TransactionService` con `@Transactional(rollbackFor = Exception.class)` en deposit/withdraw/transfer y `@Transactional(readOnly = true)` en history. Cada operación de escritura: verificar propiedad → validar fondos → actualizar balances → guardar una `Transaction` (`COMPLETED`, `Instant.now()`) en la misma transacción. Excepción de dominio ⇒ rollback (balances y filas intactos, verificado por test con `count()` y balances leídos por HTTP).
- Ownership: `owned(id, ownerId)` usa `findById` + `filter(ownerId)`. Cuenta ajena o inexistente ⇒ `IllegalArgumentException` ⇒ 404. Fondos insuficientes ⇒ `IllegalStateException` ⇒ 400. Sin JWT ⇒ 401 (filtro de seguridad).
- `TransactionController` con `@Controller("/api")`: así una sola clase publica `/api/transactions/*` y `/api/accounts/{accountId}/transactions` con las rutas exactas de la spec. `@Secured(SecurityRule.IS_AUTHENTICATED)` imprescindible (si no, 403). Dueño leído del claim `userId` del JWT, nunca del body.
- DTOs: `TransactionRequest`/`TransferRequest`/`TransactionResponse` (`@Serdeable`). `description` opcional; en la respuesta `fromAccount` es null en depósitos y `toAccount` en retiros. `TransactionResponse.from(Transaction)` centraliza el mapeo.
- **Lección importante:** Micronaut Data 4.14.3 reconstruye los records de entidad con componentes NOT NULL por defecto. Los campos SQL nullable (`from_account_id`, `to_account_id`, `description`) necesitan `@Nullable` (`io.micronaut.core.annotation.Nullable`) en `model/Transaction.java`, o `findByFromAccountIdOrToAccountId` falla con `Null value read for non-null constructor argument [fromAccountId]`. Sin esto el historial es imposible. No requirió cambios de esquema (V1 ya los declaraba nullable).
- `AccountService.deposit/withdraw` siguen sin filtrar por dueño pero ya no tienen rutas: la propiedad se impone en `TransactionService`. No se tocó auth, Flyway/V1/V2 ni `GlobalExceptionHandler` (spec-05).
- Sin commit: queda pendiente `feat(transactions): depósitos, retiros y transferencias ACID (spec-04)`.

## Spec-05 — errores y validación (2026-11-25)
- Payload único de error en `/api/**`: `{ "error", "message", "path", "timestamp", "fieldErrors"? }`. `fieldErrors` solo aparece en validación. El status va en la respuesta, no en el body. `timestamp` = `Instant.now()` ISO-8601.
- Tabla de códigos: 400 `VALIDATION_ERROR` (con `fieldErrors`) / `BUSINESS_RULE_VIOLATION` / `INSUFFICIENT_FUNDS`; 401 autenticación (`AUTHENTICATION_FAILED` en `/api/auth/login`; el resto lo gestiona Micronaut Security); 404 `NOT_FOUND` (inexistente/ajeno); 409 `CONFLICT` (duplicado, incluido `/api/auth/register`); 500 `INTERNAL_ERROR` (mensaje genérico, **sin stack trace ni mensaje interno** en la respuesta; el detalle solo se loguea en servidor).
- Clases: `exception/BankException` (base con `HttpStatus`) + `AccountNotFoundException`, `InsufficientFundsException`, `DuplicateResourceException`, `BusinessRuleException`; `ErrorPayload` construye el body y mantiene los códigos. `GlobalExceptionHandler` reescrito; `ValidationExceptionHandler` usa `@Replaces(io.micronaut.validation.exceptions.ConstraintExceptionHandler.class)`.
- Servicios y controllers lanzan/dejan propagar excepciones de dominio: se eliminaron los `try/catch` que traducían a 4xx. Las comprobaciones `ownerId == null → 401`, `@Secured` y `@PermitAll` quedan intactas.
- **Lección:** sin `io.micronaut.validation:micronaut-validation` + `micronaut-validation-processor` en `build.gradle.kts`, `@Valid` es un *no-op*: no hay `ConstraintViolationException` y no se puede producir `fieldErrors`. El handler integrado se llama `ConstraintExceptionHandler` (no `ConstraintViolationExceptionHandler`) y requiere `@Replaces` para ser sustituido.
- **Resuelto:** `/api/auth/**` usa el payload único; criterio 1 OK.
- Tests: `ErrorHandlingTest` (8 tests) + los 19 anteriores = **27 tests, 0 failures/errors**. Sin commit: queda pendiente `feat(errors): payload único y excepciones de dominio (spec-05)`.

## Spec-06 — observabilidad (2026-11-25)
- `micronaut-management` 4.10.21 + `endpoints.all.path=/actuator`. `/actuator/health` → 200 con `status=UP` y `details` visibles sin token (`endpoints.health.details-visible=ANONYMOUS`, `endpoints.health.sensitive=false`).
- **Lección:** `endpoints.health.details.enabled=true` no existe en Micronaut Management 4.10.x; la propiedad real es `endpoints.health.details-visible`. Con `ANONYMOUS` se muestran `status` + `details`; con `AUTHENTICATED` solo si hay principal; por defecto (`NEVER`) solo `status`.
- **Lección:** el indicador JDBC se llama `jdbc` en el payload (no `database`); `database` es una clave interna de sus detalles (`"database":"PostgreSQL"`). El de disco sí es `diskSpace`. El test busca `jdbc` y `database` de forma recursiva.
- `HealthController` público en `/health` devuelve `{"status":"UP"}`; `@PermitAll` + regla `isAnonymous()` en `intercept-url-map`.
- Swagger: `/swagger-ui/**` y `/swagger/**` ya eran anonymous; `/swagger-ui.html` necesitó un alias de recurso estático (`swagger-ui-root`) apuntando al **directorio** `classpath:META-INF/swagger/views/swagger-ui` (el resolvedor añade `index.html` si la ruta extraída es vacía; apuntar al fichero da 404). `AuthController` lleva `@Tag("Auth")` + `@Operation`; el spec vivo está en `build/classes/java/main/META-INF/swagger/bank-account-api-1.0.yml` con las 8 rutas `/api/**`.
- Logs: `net.logstash.logback:logstash-logback-encoder:8.1` con `LogstashEncoder` en `logback.xml`; nombres de campo `timestamp`, `level`, `logger`, `message` vía `<fieldNames>`. Un `LoggingEvent` creado a mano necesita `setLoggerContext(context)` para que el proveedor MDC del encoder no falle con NPE.
- Tests: `HealthControllerTest` (5) + los 27 anteriores = **32 tests, 0 failures/errors**. Sin commit: queda pendiente `feat(observability): Swagger, health y logs JSON (spec-06)`.

## Spec-07 — rate limiting (2026-11-25)
- `filter/RateLimitFilter` (`@Filter("/api/**")` + `HttpServerFilter`) con orden `ServerFilterPhase.SECURITY.after()` (39250): se ejecuta después de `SecurityFilter` para leer el principal del request. Tres ventanas fijas en memoria: IP/minuto, usuario autenticado/hora y transacciones/minuto; exceder cualquiera ⇒ 429 con `ErrorPayload` y `error=RATE_LIMIT_EXCEEDED`. **Corrección (petición del usuario):** el bucket de transacciones cubre `/api/transactions/**` **y** `GET /api/accounts/{accountId}/transactions` (helper `isTransactionPath`); ambas usan la misma clave (`user:<nombre>` o `ip:<ip>`), así que suman peticiones al mismo límite. Quedó eliminada la desviación anterior que dejaba el historial fuera.
- Defaults en `application.properties`: `ratelimit.enabled=true`, `ratelimit.per-ip.minute=100`, `ratelimit.per-user.hour=1000`, `ratelimit.transactions.minute=10`. `@Value` con los mismos defaults en el constructor del filtro.
- **Desviación conocida:** contadores en memoria, por instancia; se reinician al arrancar y no se comparten entre réplicas (no hay Redis). Documentada también en `AGENTS.md` y `NOTES.md`.
- **Lección:** `SecurityFilter` publica el `Authentication` en el atributo del request `HttpAttributes.PRINCIPAL` (constante deprecada pero funcional en 4.10.21); por eso el filtro debe ir después de él. Alternativa no usada: `SecurityService`.
- **Lección:** `HttpRequest.getRemoteAddress()` devuelve `InetSocketAddress` (nullable), no `Optional`; envolver con `Optional.ofNullable`. Se prefiere `X-Forwarded-For` como clave de IP (permite aislar tests con IPs sintéticas).
- **Lección:** `src/test/resources/application-test.yml` (SnakeYAML en classpath) desactiva el filtro para los 32 tests existentes; las `@Property` de clase de `RateLimitTest` lo reactivan con límites pequeños. `RateLimitDisabledTest` va en clase aparte para no compartir contexto/contadores.
- Tests: `RateLimitTest` (4: IP, usuario, transacciones y combinación depósito+historial) + `RateLimitDisabledTest` (1) + los 32 anteriores = **37 tests, 0 failures/errors**. Sin commit: queda pendiente `feat(ratelimit): filtro de rate limiting con 429 unificado (spec-07)`.

## Spec-08 — suite completa y cobertura (2026-11-25)
- `build.gradle.kts`: plugin `jacoco` con `toolVersion=0.8.13`. `jacocoTestReport` → HTML `build/reports/jacoco/test/html/index.html` + XML `build/reports/jacoco/test/jacocoTestReport.xml`; `jacocoTestCoverageVerification` → regla `BUNDLE` (`counter=LINE`, `COVEREDRATIO`, `minimum=0.70`) sobre `io.onstructive.micronaut.*`. Comando único: `./gradlew test jacocoTestReport jacocoTestCoverageVerification`.
- Cobertura real: **90.24% líneas (305/338)**, 93.74% instrucciones, 75.00% ramas, 92.16% métodos, 94.59% clases. Con los 37 tests anteriores ya era 86.39% líneas: el umbral de 70% **no se bajó**, se cumplió añadiendo tests.
- Tests nuevos: `AccountServiceTest` (8, servicio directo sobre BD real: create/duplicate/owner/deposit/withdraw/delete + excepciones de dominio), `TransactionServiceTest` (8, servicio directo: depósito/retiro/transferencia/historial + rollback verificado con `TransactionRepository.count()`), `BankApiIntegrationTest` (2 E2E: flujo register→login→2 cuentas→depósito→retiro→transferencia→historial→vaciar→borrar, y payloads 409/401/404/400 con `fieldErrors`). Total **55 tests, 0 failures/errors**.
- Los tests de servicio usan `@MicronautTest(transactional=false)` para ver datos confirmados, igual que los HTTP. `@Transactional` de `TransactionService` sigue activo en el bean aunque el test no esté envuelto en transacción; por eso el rollback funciona.
- **Desviaciones exigidas por la spec:** Testcontainers no usado (se comparte el contenedor Podman `bank-api-db`) y REST Assured no usado (se usa `Micronaut HttpClient` con `toBlocking().exchange`). Otras: `toolVersion` fijado a 0.8.13; regla de verificación por bundle (2 clases al 0%: `Application.main` y `AmountRequest`, fuera del flujo de negocio); `application-test.yml` mantiene `ratelimit.enabled=false` para que el E2E no reciba 429.
- Sin commit: queda pendiente `test(coverage): suite de servicio/E2E y JaCoCo al 70% (spec-08)`.

## Spec-09 — empaquetado con Podman (2026-11-25)
- `Dockerfile` multi-stage: build `eclipse-temurin:21-jdk` → `./gradlew --no-daemon --console=plain shadowJar`; final `eclipse-temurin:21-jre`, usuario no root `bankapi`, `COPY --from=build .../bankaccount-0.1-all.jar /app/bankaccount.jar`, `USER bankapi`, `ENV SERVER_PORT=8080`, `EXPOSE 8080`, `ENTRYPOINT ["java","-jar","/app/bankaccount.jar"]`. `.dockerignore` excluye `.git`, `.gradle`, `build/`, `.idea/` y similares (incluye `specs/` y `*.md`).
- El plugin `com.gradleup.shadow` 8.3.9 (vía `io.micronaut.application`) produce `build/libs/bankaccount-0.1-all.jar` (~25 MB); el nombre incluye `-all`, no es el JAR plano. La etapa final solo necesita ese fichero.
- Configuración nueva: `micronaut.server.port=${SERVER_PORT:8080}` en `src/main/resources/application.properties`. Permite cambiar el puerto dentro del contenedor con `-e SERVER_PORT=...` sin reconstruir la imagen; el default sigue siendo 8080.
- Comandos: `podman pull docker.io/library/eclipse-temurin:21-jdk` y `...:21-jre` (una vez); `podman build -t bank-api:spec09 .`; `podman run -d --rm --name bank-api-smoke09 --network host -e SERVER_PORT=8080 bank-api:spec09`; `curl http://127.0.0.1:8080/health` → `200 {"status":"UP"}`; `podman stop bank-api-smoke09` (con `--rm` se autodestruye). Imagen resultante `localhost/bank-api:spec09` (~400 MB).
- `--network host` es necesario en el smoke test porque el datasource apunta a `127.0.0.1:5432` (`bank-api-db` en el host). En producción: red compartida o `BANK_DB_HOST` adecuado. No se tocó `bank-api-db` ni otros contenedores/volúmenes.
- **Desviaciones:** `./gradlew nativeImage` documentado como opcional pero **no ejecutado** (GraalVM no instalada; se entrega JVM fat JAR, no binario nativo). Imagen Ubuntu no alpine (~400 MB), sin optimizar. El build en contenedor descarga Gradle 8.14.3 y dependencias la primera vez (~2 min); los warnings SLF4J y `HttpAttributes.PRINCIPAL` deprecado aparecen pero no bloquean.
- Verificado tras el cambio: 55 tests, 0 failures/errors, `jacocoTestCoverageVerification` OK y cobertura 90.24% líneas (305/338) sin cambios. Sin commit: queda pendiente `build(docker): imagen multi-stage y smoke test de /health (spec-09)`.
