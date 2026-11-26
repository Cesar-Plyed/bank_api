# Spec-10 — Cierre documental

## Objetivo
Hacer que `README.md`, `NOTES.md` y `MEMORY.md` describan el estado real del repo al final de la implementación.

## Alcance
- Reescribir del README las secciones que ya no son verdaderas:
  - **Tech Stack:** Micronaut Security (no Spring Security), Podman (no Docker), Testcontainers/REST Assured no usados.
  - **Quick Start:** JDK 21 + `JAVA_HOME`, PostgreSQL 15 en Podman, `JWT_SECRET`, `./gradlew test`, `./gradlew run` y `podman build`.
  - **Estructura:** paquetes reales (`controller`, `service`, `model` + enums, `repository`, `security`, `exception`, `filter`, `dto`, `db/migration`) y `specs/`.
  - **API Endpoints:** `/api/auth/**`, `/api/accounts`, `/api/transactions/**`, `/api/accounts/{id}/transactions`, `/health`, `/actuator/health`, payload de errores y 429.
  - **Configuración:** `application.properties` (no YAML), `BANK_DB_*`, `JWT_SECRET`, `SERVER_PORT`, `ratelimit.*`; default de JWT solo para dev.
  - **Seguridad:** JWT HS256, `@Secured`, propiedad por `userId`, sin refresh tokens.
  - **Testing:** `./gradlew test jacocoTestReport jacocoTestCoverageVerification`, 55 tests, 90.24% líneas, BD de Podman requerida.
  - **Performance:** quitar benchmarks no medidos y marcar `nativeImage` como opcional/no ejecutado.
  - **Roadmap:** marcar `[x]` en "Tests completos"; dejar sin marcar los ítems fuera de alcance.
- No inventar métricas: si no se midió, quitar la cifra o decir "no medido".
- `NOTES.md` y `MEMORY.md`: cierre con lista de specs 00-09, commits y desviaciones finales.
- Verificar con `grep` que no queden afirmaciones falsas (Spring Security, Docker, `application.yml`, `/accounts` legacy, Testcontainers como herramienta usada).
- No crear `LICENSE`; si el README lo menciona, documentar como pendiente.

## Fuera de alcance
Cambios de código, commits, push, borrados.

## Criterios de aceptación
1. El README describe rutas, seguridad, BD, tests y comandos reales; sin afirmaciones falsas verificables.
2. El roadmap tiene `[x]` en "Tests completos".
3. `NOTES.md` y `MEMORY.md` incluyen el cierre con specs, commits y desviaciones.
4. `./gradlew test jacocoTestCoverageVerification` sigue pasando (55 tests, 0 failures).
5. No hay cambios en `src/` ni en `build.gradle.kts`.

## Comandos de verificación
`grep -rn 'Spring Security\|Docker\|application.yml\|/accountslegacy' README.md` · `./gradlew test jacocoTestCoverageVerification`

## Definition of Done
Reportar archivos modificados, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
