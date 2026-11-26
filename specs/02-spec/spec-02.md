# Spec-02 — Autenticación JWT

## Objetivo
Registro de usuarios y login con JWT firmado, contraseñas BCrypt y endpoints `/api/auth/**` públicos.

## Alcance
- Habilitar Micronaut Security + JWT (`micronaut.security.enabled=true`, secret desde `JWT_SECRET` de al menos 256 bits, expiración 3600 s). Añadir `micronaut-security-jwt` y la dependencia BCrypt si faltan.
- DTOs `@Serdeable` con validación: `RegisterRequest`, `LoginRequest`, `LoginResponse`, `UserResponse`.
- `PasswordEncoder` BCrypt (10 rounds) y `AuthenticationProvider` que valide credenciales contra `UserRepository`.
- `AuthController` en `/api/auth`:
  - `POST /register` → 201, guarda `User` con `passwordHash`, rechaza username/email duplicados.
  - `POST /login` → 200 con `accessToken`, `tokenType=Bearer`, `expiresIn=3600` y `user` (id, username, email).
  - Ambos marcados como públicos (`@PermitAll`).
- `JwtTokenService` que firme el token con el username como subject.
- Tests `@MicronautTest` + `HttpClient`: registro OK, registro duplicado 4xx, login OK con token no vacío, login con password incorrecto 401.
- Documentar `JWT_SECRET` y el cambio de seguridad en `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Fuera de alcance
- Migrar `/accounts` a `/api/accounts` y su ownership (spec-03), transacciones (spec-04), manejo global de errores (spec-05), rate limiting (spec-06), commits.

## Criterios de aceptación
1. `micronaut.security.enabled=true` con secret configurable por `JWT_SECRET` (default solo para dev, documentado).
2. `./gradlew test` pasa con los 4 tests nuevos y los existentes (`BankaccountTest`, `RepositoryTest`): 0 failures/errors.
3. El token devuelto por login es un JWT de 3 segmentos y el password nunca se almacena en claro (hash BCrypt `$2a$`/`$2b$`).
4. `/api/auth/register` y `/api/auth/login` responden sin necesidad de token.
5. `AGENTS.md`, `NOTES.md` y `MEMORY.md` reflejan la seguridad activa y `JWT_SECRET`.

## Comandos de verificación
`./gradlew test --tests "*Auth*Test"` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
