# Bank API Simulator

![Java](https://img.shields.io/badge/Java-21-ED8936?logo=java&logoColor=white)
![Micronaut](https://img.shields.io/badge/Micronaut-4.10.12-FF6B00?logo=micronaut&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.14.3-02303A?logo=gradle&logoColor=white)
![Swagger](https://img.shields.io/badge/Swagger-UI-85EA2D?logo=swagger&logoColor=white)
![Status](https://img.shields.io/badge/Status-Active-brightgreen)

API REST para simulación bancaria construida con Micronaut 4.10.12 y Java 21. Cubre registro/login JWT, CRUD de cuentas por dueño, transacciones atómicas (depósito, retiro, transferencia), historial, manejo uniforme de errores, rate limiting, health checks y logs estructurados.

> **Nota de estado:** este README describe el **código y la configuración reales** del repo. Las cifras citadas (tests, cobertura, tamaño del JAR) son mediciones verificadas; lo que no se midió se indica como tal. El desarrollo sigue Spec Driven Development (SDD): specs en `specs/NN-spec/`, detalle en `NOTES.md` y `MEMORY.md`.

---

## Tabla de Contenidos

- [Características](#características)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Estructura del Proyecto](#estructura-del-proyecto)
- [API Endpoints](#api-endpoints)
- [Configuración](#configuración)
- [Seguridad](#seguridad)
- [Transacciones](#transacciones)
- [Manejo de errores](#manejo-de-errores)
- [Testing](#testing)
- [Observabilidad](#observabilidad)
- [Empaquetado](#empaquetado)
- [Performance](#performance)
- [Troubleshooting](#troubleshooting)
- [Roadmap](#roadmap)
- [Contribuir](#contribuir)
- [Licencia](#licencia)

---

## Características

### Gestión Bancaria
- Crear, listar, consultar y borrar cuentas, siempre limitadas al dueño autenticado.
- Depósitos, retiros y transferencias con validación y rollback en caso de error.
- Historial de movimientos de una cuenta (en ambos sentidos).
- Balances con `BigDecimal` e identificadores `UUID`.

### Seguridad
- Autenticación JWT firmada con HS256 (**Micronaut Security**, no Spring Security).
- Contraseñas hasheadas con BCrypt (10 rondas); el hash nunca se expone.
- Los endpoints `/api/accounts` y `/api/transactions/**` exigen `@Secured(SecurityRule.IS_AUTHENTICATED)` y filtran por el claim `userId` del JWT.
- `/api/auth/**` es público (`@PermitAll`).
- Validación de entrada con `micronaut-validation` + processor (`@Valid` + `@NotNull`, `@Positive`, `@Pattern`, etc.).
- Rate limiting por IP, por usuario y para operaciones de transacción.

### Base de Datos
- PostgreSQL 15 en Podman, acceso JDBC con Hikari.
- Flyway es el único dueño del esquema (`datasources.default.schema-generate=NONE`), con migraciones `V1__CreateTables.sql` y `V2__AddIndexes.sql`.
- `@Transactional` por operación de escritura: fondos insuficientes o cuenta ajena revierten cambios y no dejan filas de `Transaction`.
- Índices sobre `owner_id` y columnas de `transactions` (ver `V2__AddIndexes.sql`).

### API Profesional
- Swagger UI anónima en `/swagger-ui.html` y `/swagger-ui/**`; especificación YAML servida en `/swagger/**`.
- Respuestas de error con un payload único para `/api/**`.
- Logs JSON por línea (Logstash encoder).
- Health checks: `/health` y `/actuator/health`.

---

## Tech Stack

```
BACKEND
- Java 21 LTS (Temurin)
- Micronaut 4.10.12
- Gradle wrapper 8.14.3 (plugin io.micronaut.application 4.6.2)

SEGURIDAD
- Micronaut Security 4.15.2 + micronaut-security-jwt (HS256)
- BCrypt (org.mindrot:jbcrypt 0.4) — bean propio PasswordEncoder
- Sin refresh tokens

BASE DE DATOS
- PostgreSQL 15 (contenedor Podman bank-api-db)
- Flyway 10.22.0 (+ flyway-database-postgresql)
- Micronaut Data JDBC + Hikari CP

DOCUMENTACION
- Swagger UI / OpenAPI generado por micronaut-openapi
- Spec vivo: build/classes/java/main/META-INF/swagger/bank-account-api-1.0.yml

BUILD & RUNTIME
- Fat JAR con com.gradleup.shadow 8.3.9 (build/libs/bankaccount-0.1-all.jar, ~25 MB)
- Dockerfile multi-stage construido con Podman
- GraalVM nativeImage: opcional, NO ejecutado (GraalVM no instalada)

OBSERVABILIDAD
- micronaut-management (/actuator/health)
- logstash-logback-encoder 8.1 (logs JSON)

TESTING
- JUnit 5 + Micronaut HttpClient
- JaCoCo 0.8.13 (cobertura y verificación >= 70% de líneas)
- Testcontainers y REST Assured: NO usados (se comparte el contenedor Podman y se usa Micronaut HttpClient)
```

---

## Quick Start

### Prerequisitos

```bash
# JDK 21 (esta máquina no tiene java en PATH)
export JAVA_HOME=/home/void/AI/apps/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"

# Podman 6.1.4 (no Docker) para la base de datos
podman --version
```

No hay PostgreSQL instalado en el host: se usa un contenedor Podman. Gradle se ejecuta mediante el wrapper incluido (`./gradlew`), que descarga la distribución 8.14.3 la primera vez.

### 1. Arrancar PostgreSQL 15 en Podman

```bash
podman pull docker.io/library/postgres:15   # una sola vez
podman run -d --name bank-api-db \
  -e POSTGRES_DB=bank_api \
  -e POSTGRES_USER=bank_user \
  -e POSTGRES_PASSWORD=bank_pass \
  -p 127.0.0.1:5432:5432 \
  docker.io/library/postgres:15

podman ps
podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'   # V1/V2 aplicadas por Flyway
```

El esquema lo crea Flyway en el primer arranque de la app (`schema-generate=NONE`). Parar/reiniciar: `podman stop bank-api-db` / `podman start bank-api-db`. **No borrar el contenedor ni su volumen.**

### 2. Ejecutar tests y arrancar la app

```bash
export JAVA_HOME=/home/void/AI/apps/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew test                                   # 55 tests, requiere bank-api-db activo
./gradlew test jacocoTestReport jacocoTestCoverageVerification   # + reporte de cobertura

# Definir un secreto JWT propio (el default solo es de desarrollo)
export JWT_SECRET='cambia-este-valor-por-al-menos-32-bytes-256-bits'
./gradlew run
```

Acceder a Swagger UI en `http://localhost:8080/swagger-ui.html` y al health check en `http://localhost:8080/health`.

### 3. Construir la imagen con Podman

```bash
podman build -t bank-api:spec09 .
podman run -d --rm --name bank-api-smoke09 --network host -e SERVER_PORT=8080 bank-api:spec09
curl http://127.0.0.1:8080/health       # 200 {"status":"UP"}
podman stop bank-api-smoke09            # con --rm se autodestruye
```

`--network host` es necesario en el smoke test porque el datasource apunta a `127.0.0.1:5432` (el `bank-api-db` del host). En producción hace falta una red compartida o un `BANK_DB_HOST` apropiado.

---

## Estructura del Proyecto

```
bank_api/
├── src/
│   ├── main/
│   │   ├── java/io/onstructive/micronaut/
│   │   │   ├── Application.java                 # Punto de entrada
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java          # /api/auth (register, login)
│   │   │   │   ├── AccountController.java       # /api/accounts (CRUD por dueño)
│   │   │   │   ├── TransactionController.java   # /api (transacciones + historial)
│   │   │   │   └── HealthController.java        # /health
│   │   │   ├── service/
│   │   │   │   ├── AccountService.java
│   │   │   │   └── TransactionService.java      # @Transactional por operación
│   │   │   ├── model/
│   │   │   │   ├── User.java, Account.java, Transaction.java
│   │   │   │   ├── AccountType.java, AccountStatus.java
│   │   │   │   ├── TransactionType.java, TransactionStatus.java
│   │   │   │   └── (los DTOs viven en el paquete dto/, ver abajo)
│   │   │   ├── dto/
│   │   │   │   ├── RegisterRequest.java, LoginRequest.java
│   │   │   │   ├── LoginResponse.java, UserResponse.java
│   │   │   │   ├── CreateAccountRequest.java
│   │   │   │   ├── TransactionRequest.java, TransferRequest.java
│   │   │   │   ├── TransactionResponse.java, AmountRequest.java
│   │   │   ├── repository/
│   │   │   │   ├── UserRepository.java, AccountRepository.java
│   │   │   │   └── TransactionRepository.java   # todos con Dialect.POSTGRES
│   │   │   ├── security/
│   │   │   │   ├── PasswordEncoder.java         # BCrypt 10 rondas
│   │   │   │   ├── UserAuthenticationProvider.java
│   │   │   │   └── JwtTokenService.java
│   │   │   ├── exception/
│   │   │   │   ├── BankException.java + 4 excepciones de dominio
│   │   │   │   ├── ErrorPayload.java            # payload único
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   └── ValidationExceptionHandler.java
│   │   │   └── filter/
│   │   │       └── RateLimitFilter.java         # @Filter("/api/**")
│   │   └── resources/
│   │       ├── application.properties           # configuración real (no YAML)
│   │       ├── logback.xml                      # logs JSON
│   │       ├── openapi/bankaccount-0.0.yml       # placeholder obsoleto
│   │       └── db/migration/
│   │           ├── V1__CreateTables.sql
│   │           └── V2__AddIndexes.sql
│   └── test/
│       ├── java/io/onstructive/micronaut/       # 12 clases de test (55 tests)
│       └── resources/application-test.yml       # ratelimit.enabled=false
├── specs/                                       # specs SDD 00..10 (spec-NN.md + agent.md)
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── Dockerfile, .dockerignore
├── logback.xml, openapi.properties
├── AGENTS.md, NOTES.md, MEMORY.md, README.md
└── LICENSE                                      # NO existe todavía (pendiente)
```

El spec OpenAPI de verdad se genera en `build/classes/java/main/META-INF/swagger/bank-account-api-1.0.yml`; `src/main/resources/openapi/bankaccount-0.0.yml` es un placeholder obsoleto y no es fuente de verdad.

---

## API Endpoints

Base: `http://localhost:8080`. Los bodies de petición/respuesta usan `application/json`. Todos los IDs son `UUID`.

### Autenticación (público)

#### Registrar usuario — `POST /api/auth/register`

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"juan","email":"juan@example.com","password":"SecurePass123!"}'
```

`201 Created`:
```json
{ "id": "<uuid>", "username": "juan", "email": "juan@example.com" }
```
Si username o email ya existen: `409` con `error=CONFLICT`.

#### Login — `POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"juan","password":"SecurePass123!"}'
```

`200 OK`:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": "<uuid>", "username": "juan", "email": "juan@example.com" }
}
```
Credenciales inválidas: `401` con `error=AUTHENTICATION_FAILED`.

### Cuentas (requieren JWT)

Todas exigen `Authorization: Bearer <TOKEN>` y operan solo sobre cuentas del usuario del claim `userId`. La ruta legacy `/accounts` ya no existe.

#### Crear cuenta — `POST /api/accounts`

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountNumber":"1001234567","currency":"USD","accountType":"SAVINGS","initialBalance":1000.00}'
```

`201 Created` (el `ownerId` se toma del JWT, nunca del body):
```json
{
  "id": "<uuid>", "ownerId": "<uuid>", "accountNumber": "1001234567",
  "currency": "USD", "balance": 1000.00, "accountType": "SAVINGS",
  "status": "ACTIVE", "createdAt": "2026-11-25T10:30:00Z"
}
```
`accountType` es `CHECKING` o `SAVINGS`; `currency` son 3 letras mayúsculas; `initialBalance` es opcional (por defecto 0).

#### Listar / consultar / borrar

```bash
GET    /api/accounts                  # 200 [Account...] (solo propias)
GET    /api/accounts/{id}             # 200 Account; 404 si no existe o es ajena
DELETE /api/accounts/{id}             # 204 si saldo=0; 400 si tiene saldo; 404 si ajena/inexistente
```

### Transacciones (requieren JWT)

El dueño se lee del claim `userId` del JWT, nunca del body.

#### Depósito — `POST /api/transactions/deposit`

```bash
curl -X POST http://localhost:8080/api/transactions/deposit \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"accountId":"<uuid>","amount":500.00,"description":"Depósito de nómina"}'
```

`201 Created`:
```json
{
  "id": "<uuid>", "fromAccount": null, "toAccount": "<uuid>",
  "amount": 500.00, "type": "DEPOSIT", "status": "COMPLETED",
  "timestamp": "2026-11-25T16:00:00Z", "description": "Depósito de nómina"
}
```

#### Retiro — `POST /api/transactions/withdraw`

Body: `{"accountId":"<uuid>","amount":100.00,"description":"..."}`.
`201 Created` con `type=WITHDRAWAL`, `fromAccount=<uuid>`, `toAccount=null`. Fondos insuficientes → `400` con `error=INSUFFICIENT_FUNDS`.

#### Transferencia — `POST /api/transactions/transfer`

Body: `{"fromAccountId":"<uuid>","toAccountId":"<uuid>","amount":250.00,"description":"..."}`.
`201 Created` con `type=TRANSFER`, ambas cuentas definidas. Cuenta ajena/inexistente → `404`; transferir a la misma cuenta → `400 BUSINESS_RULE_VIOLATION`.

#### Historial — `GET /api/accounts/{accountId}/transactions`

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/api/accounts/<uuid>/transactions
```

`200 OK` con un array de `TransactionResponse` (movimientos donde la cuenta es origen o destino). Cuenta ajena/inexistente → `404`.

### Health

```bash
GET /health            # 200 {"status":"UP"}  (público)
GET /actuator/health   # público, con status=UP y detalles
```

Ejemplo simplificado de `/actuator/health`:
```json
{
  "name": "bankaccount", "status": "UP",
  "details": {
    "diskSpace": { "status": "UP", "details": { "total": ..., "free": ..., "threshold": 10485760 } },
    "jdbc": { "status": "UP", "details": { "database": "PostgreSQL", "version": "15.20 ..." } }
  }
}
```
El indicador JDBC se llama `jdbc` (con `database` como clave interna), no `database`. Los detalles son visibles sin token (`endpoints.health.details-visible=ANONYMOUS`).

---

## Configuración

La configuración real vive en `src/main/resources/application.properties` (**no** en `application.yml`). Fragmentos relevantes:

```properties
datasources.default.dialect=POSTGRES
datasources.default.url=jdbc:postgresql://${BANK_DB_HOST:127.0.0.1}:${BANK_DB_PORT:5432}/${BANK_DB_NAME:bank_api}
datasources.default.username=${BANK_DB_USER:bank_user}
datasources.default.password=${BANK_DB_PASSWORD:bank_pass}
datasources.default.schema-generate=NONE
flyway.datasources.default.enabled=true

micronaut.application.name=bankaccount
micronaut.server.port=${SERVER_PORT:8080}

micronaut.security.enabled=true
micronaut.security.token.jwt.signatures.secret.generator.secret=${JWT_SECRET:dev-only-secret-change-me-please-32bytes}
micronaut.security.token.jwt.generator.access.expiration=3600

endpoints.all.path=/actuator
endpoints.health.enabled=true
endpoints.health.sensitive=false
endpoints.health.details-visible=ANONYMOUS

ratelimit.enabled=true
ratelimit.per-ip.minute=100
ratelimit.per-user.hour=1000
ratelimit.transactions.minute=10
```

### Variables de entorno

```bash
# Base de datos (defaults incluidos)
BANK_DB_HOST=127.0.0.1
BANK_DB_PORT=5432
BANK_DB_NAME=bank_api
BANK_DB_USER=bank_user
BANK_DB_PASSWORD=bank_pass

# Seguridad: definir SIEMPRE en producción (>= 32 bytes / 256 bits para HS256)
JWT_SECRET='valor-secreto-de-al-menos-32-bytes'

# Servidor
SERVER_PORT=8080
```

El valor por defecto de `JWT_SECRET` existe **solo para desarrollo y tests**; en producción debe sustituirse. La URL JDBC se compone con placeholders sin colon en el default (un único `${BANK_DB_URL:jdbc:...}` se resolvía mal en Micronaut). En tests, `src/test/resources/application-test.yml` fija `ratelimit.enabled=false`.

---

## Seguridad

- **Micronaut Security** (no Spring Security): `micronaut.security.enabled=true` y `micronaut-security-jwt` con firma HS256.
- El token JWT viaja en `Authorization: Bearer <token>`; expira en 3600 s (1 hora).
- `sub` = username; atributos `userId` (UUID en texto) y `email`. El dueño de cada recurso se deduce del claim `userId`, nunca del body.
- `/api/auth/**` es público (`@PermitAll`). `/api/accounts`, `/api/transactions/**` y `/api/accounts/{id}/transactions` llevan `@Secured(SecurityRule.IS_AUTHENTICATED)`: sin token → `401`; con token pero sin esta anotación Micronaut respondería `403`.
- Swagger UI, `/health` y `/actuator/**` son anónimos vía `intercept-url-map`.
- Contraseñas hasheadas con BCrypt 10 rondas (`$2a$`/`$2b$`), nunca en texto plano.
- **Refresh tokens: no implementados** (fuera de alcance del roadmap).

### Rate limiting

`RateLimitFilter` (`@Filter("/api/**")`, orden posterior al filtro de seguridad) aplica ventanas fijas en memoria:

| Ámbito | Límite por defecto |
| --- | --- |
| IP (por minuto) | 100 |
| Usuario autenticado (por hora) | 1000 |
| Transacciones (por minuto) | 10 |

El bucket de transacciones cubre tanto `/api/transactions/**` como `GET /api/accounts/{accountId}/transactions` y comparten contador. La IP se resuelve de `X-Forwarded-For` (con fallback al socket). Al excederse → `429` con `error=RATE_LIMIT_EXCEEDED` y el payload único.

**Desviación:** los contadores son en memoria, por instancia: se reinician al arrancar y no se comparten entre réplicas (no hay Redis).

---

## Transacciones

`TransactionService` aplica `@Transactional(rollbackFor = Exception.class)` a `deposit`, `withdraw` y `transfer`, y `@Transactional(readOnly = true)` a `history`. Cada operación de escritura: verificar propiedad → validar fondos → actualizar balances → guardar exactamente una `Transaction` (`status=COMPLETED`, `Instant.now()`) en la misma transacción. Si falla una regla de negocio, todo se revierte (balances y filas intactos, verificado por tests con `TransactionRepository.count()`).

```java
// Esquema de TransactionService (simplificado)
@Transactional(rollbackFor = Exception.class)
public Transaction transfer(UUID fromId, UUID toId, BigDecimal amount, UUID ownerId) {
    Account from = owned(fromId, ownerId);   // 404 si no existe o es ajena
    Account to   = owned(toId, ownerId);     // 404 si no existe o es ajena
    if (from.balance().compareTo(amount) < 0) {
        throw new InsufficientFundsException();  // 400 -> rollback
    }
    // actualizar balances y guardar una sola Transaction COMPLETED
    ...
}
```

No se usan FK entre tablas (decisión de diseño fuera de alcance); `owner_id`, `from_account_id` y `to_account_id` son `UUID`.

---

## Manejo de errores

Todas las respuestas de error bajo `/api/**` usan un único payload (incluidos los errores de autenticación y el 429):

```json
{
  "error": "NOT_FOUND",
  "message": "<texto legible>",
  "path": "/api/accounts/<uuid>",
  "timestamp": "2026-11-25T16:00:00Z",
  "fieldErrors": { "amount": "must be positive" }
}
```

`fieldErrors` es opcional: solo aparece en errores de validación (400). El status HTTP viaja en la respuesta, no en el cuerpo.

| Situación | HTTP | `error` |
| --- | --- | --- |
| Cuenta inexistente o ajena | 404 | `NOT_FOUND` |
| Fondos insuficientes | 400 | `INSUFFICIENT_FUNDS` |
| Regla de negocio (borrar con saldo, transferir a la misma cuenta) | 400 | `BUSINESS_RULE_VIOLATION` |
| Body inválido (`@Valid`) | 400 | `VALIDATION_ERROR` (con `fieldErrors`) |
| Recurso duplicado | 409 | `CONFLICT` |
| Credenciales inválidas en login | 401 | `AUTHENTICATION_FAILED` |
| Sin token / token inválido | 401 | gestionado por Micronaut Security |
| Error inesperado | 500 | `INTERNAL_ERROR` (mensaje genérico, sin stack trace) |
| Exceso de peticiones | 429 | `RATE_LIMIT_EXCEEDED` |

Los controllers ya no traducen excepciones con `try/catch`: lo hace `GlobalExceptionHandler`, con `ValidationExceptionHandler` (`@Replaces`) para las violaciones de `@Valid`.

---

## Testing

La suite requiere el contenedor `bank-api-db` activo en `127.0.0.1:5432`; si la BD no está arriba, `./gradlew test` falla.

```bash
./gradlew test                                   # 55 tests, 0 failures/errors en 12 clases
./gradlew test --tests "*Auth*Test"               # subconjuntos
./gradlew test --tests "*Account*Test"
./gradlew test --tests "*Transaction*Test"
./gradlew test --tests "*Error*Test"
./gradlew test --tests "*Health*Test"
./gradlew test --tests "*RateLimit*Test"

./gradlew test jacocoTestReport jacocoTestCoverageVerification
```

Clases de test: `BankaccountTest` (smoke), `RepositoryTest`, `AuthControllerTest` (4), `AccountControllerTest` (6), `TransactionControllerTest` (7), `ErrorHandlingTest` (8), `HealthControllerTest` (5), `RateLimitTest` (4), `RateLimitDisabledTest` (1), `AccountServiceTest` (8), `TransactionServiceTest` (8) y `BankApiIntegrationTest` (2 E2E). Los tests HTTP y de servicio usan `@MicronautTest(transactional=false)` para que el cliente vea datos confirmados.

**Cobertura real (2026-11-25, umbral de 70% de líneas no rebajado):**

| Contador | Cubiertos | Total | Porcentaje |
| --- | --- | --- | --- |
| Línea | 305 | 338 | **90.24%** |
| Instrucción | 1512 | 1613 | 93.74% |
| Rama | 84 | 112 | 75.00% |

Reportes: `build/reports/jacoco/test/html/index.html` y `build/reports/jacoco/test/jacocoTestReport.xml`.

**Desviaciones:** Testcontainers no se usa (se comparte el contenedor Podman `bank-api-db`) y REST Assured no se usa (se usa `Micronaut HttpClient` con `toBlocking()`). Dos clases quedan al 0% de líneas (`Application.main` y el DTO `AmountRequest`, fuera del flujo de negocio), pero la verificación es por bundle y el global supera el umbral.

---

## Observabilidad

- `/health`: endpoint público que devuelve `{"status":"UP"}`.
- `/actuator/health`: expone `status=UP` y `details` (indicador de disco `diskSpace` y JDBC bajo `jdbc`, con `database` como detalle interno). Los detalles son visibles sin token (`endpoints.health.details-visible=ANONYMOUS`).
- Swagger UI anónima en `/swagger-ui.html` y `/swagger-ui/**`; el YAML generado se sirve en `/swagger/**`. `AuthController` lleva `@Tag`/`@Operation` para documentar registro y login.
- Logs JSON por línea con Logstash encoder: `timestamp`, `level`, `logger`, `message` (más `@version`, `thread`, `level_value`), configurados en `logback.xml`.

---

## Empaquetado

`Dockerfile` multi-stage: etapa de build con `eclipse-temurin:21-jdk` ejecuta `./gradlew --no-daemon --console=plain shadowJar`; etapa final con `eclipse-temurin:21-jre` crea el usuario no root `bankapi`, copia `bankaccount-0.1-all.jar` como `/app/bankaccount.jar`, expone 8080 y usa `ENTRYPOINT ["java","-jar","/app/bankaccount.jar"]`. `.dockerignore` excluye `.git`, `.gradle`, `build/`, `.idea/`, `specs/` y `*.md`.

```bash
./gradlew shadowJar                 # build/libs/bankaccount-0.1-all.jar (~25 MB, medido)
podman build -t bank-api:spec09 .
```

La imagen construida es `localhost/bank-api:spec09` (~400 MB). La variable `SERVER_PORT` (default 8080) permite cambiar el puerto dentro del contenedor sin reconstruir la imagen.

---

## Performance

No se publican cifras de latencia, memoria ni throughput: **no fueron medidas**. Lo que sí está verificado:

- Micronaut 4.10.12 sobre Netty; `TaskExecutors.IO` en los controllers con acceso JDBC.
- Pool Hikari para PostgreSQL y índices en `V2__AddIndexes.sql` (`owner_id`, columnas de `transactions`).
- Fat JAR ejecutable sobre JRE (~25 MB, tamaño medido del artefacto).
- Compilación nativa GraalVM (`./gradlew nativeImage`) queda como **opcional y no ejecutada**: esta máquina solo tiene Temurin 21, se distribuye el fat JAR, no un binario nativo. Tampoco se midió el arranque de la imagen nativa.

---

## Troubleshooting

| Error | Solución |
| --- | --- |
| `Connection refused` / fallo de tests | Verificar `podman ps` y que `bank-api-db` esté activo; si no, `podman start bank-api-db`. |
| `Invalid JDBC URL` / `Unsupported Database: PostgreSQL` | Revisar `BANK_DB_*` y que `flyway-database-postgresql` esté en el build; la URL debe componerse con placeholders sin colon en el default. |
| `401 Unauthorized` | Falta o expiró el JWT: hacer login de nuevo y enviar `Authorization: Bearer <token>`. |
| `403 Forbidden` con token válido | La ruta no tiene regla `@Secured`/`@PermitAll` explícita; verificar la anotación del controller. |
| `400 INSUFFICIENT_FUNDS` | Balance insuficiente en la cuenta. |
| `404 NOT_FOUND` | Cuenta inexistente o ajena al `userId` del JWT. |
| `429 RATE_LIMIT_EXCEEDED` | Se superó alguna ventana; esperar a que se reinicie el bucket (en memoria). |
| `./gradlew` falla al arrancar | Exportar `JAVA_HOME=/home/void/AI/apps/jdk-21` y añadir `$JAVA_HOME/bin` al `PATH`. |

---

## Roadmap

- [x] Autenticación JWT básica
- [x] CRUD de cuentas
- [x] Transacciones (depósito, retiro, transferencia)
- [x] Swagger UI
- [x] Flyway migrations
- [x] Tests completos (JUnit 5 + Micronaut HttpClient + JaCoCo; 55 tests, 90.24% líneas)
- [ ] Refresh tokens
- [ ] Auditoría de transacciones
- [ ] API versioning
- [ ] GraphQL support
- [ ] Websockets para notificaciones
- [ ] Reportes financieros

> Testcontainers y REST Assured aparecían en el roadmap aspiracional, pero **no se usaron**: la suite comparte el contenedor Podman y usa `Micronaut HttpClient`. "Tests completos" se refiere a la cobertura real indicada arriba.

---

## Contribuir

El desarrollo sigue SDD: cada spec vive en `specs/NN-spec/` con `spec-NN.md` y `agent.md`; un sub agente implementa sin commitear, el orquestador verifica y aprueba, y se hace un commit por spec con Conventional Commits (`feat`, `fix`, `docs`, `test`, `refactor`, `build`, etc.). No se hacen push, reset ni borrados sin autorización explícita.

---

## Licencia

**Pendiente:** no existe archivo `LICENSE` en el repo. La mención anterior a "MIT License" no estaba respaldada por ningún fichero; se documenta aquí como tarea pendiente en lugar de inventar una licencia.
