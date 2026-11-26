# Spec-11 — Preparación para GitHub (release prep)

## Objetivo
Dejar el repo portable, con licencia, esquema íntegro y documentación fiel antes de publicarlo en GitHub.

## Alcance
1. **README portable:** sustituir rutas locales (`/home/void/AI/apps/jdk-21`, "esta máquina no tiene java en PATH", "no hay PostgreSQL en el host") por instrucciones genéricas (`$JAVA_HOME`, JDK 21, Podman o PostgreSQL local vía `BANK_DB_*`); añadir `git clone https://github.com/Cesar-Plyed/bank_api.git`; corregir el árbol (`logback.xml` está en `src/main/resources/`, `LICENSE` ya existirá); mantener las cifras verificadas.
2. **Notas reales:** `NOTES.md` y `MEMORY.md` deben reflejar los 11 commits ejecutados (hashes y mensajes reales de `git log`); eliminar o marcar como históricas las líneas que aún dicen "commit pendiente".
3. **Limpieza de build:** eliminar `runtimeOnly("com.h2database:h2")` de `build.gradle.kts` (sin uso).
4. **LICENSE:** crear `LICENSE` MIT con `Copyright (c) 2026 Cesar Plyed`; actualizar la sección Licencia del README.
5. **Foreign keys (usuario autorizó borrar solo huérfanos):** antes de aplicar la migración, eliminar exactamente las filas huérfanas verificadas (88 `accounts` sin usuario; 4 `transactions.from_account_id` y 4 `transactions.to_account_id` sin cuenta) usando `podman exec bank-api-db psql ...`; crear `V3__AddForeignKeys.sql` con FK de `accounts.owner_id → users(id)`, `transactions.from_account_id → accounts(id)` y `transactions.to_account_id → accounts(id)`. Verificar que no quedan huérfanos y que la BD conserva el resto de filas. **No tocar `bank-api-db` ni otros contenedores/volúmenes.**
6. **Borrados autorizados:** eliminar `src/main/resources/openapi/bankaccount-0.0.yml` (placeholder obsoleto) y `src/main/java/io/onstructive/micronaut/dto/AmountRequest.java` (sin usos). Enumerar en `NOTES.md` qué se borró y qué se conservó.
7. **Warning deprecated:** sustituir `HttpAttributes.PRINCIPAL` en `RateLimitFilter` por una API no deprecada (p. ej. `SecurityService` o el accessor equivalente de Micronaut 4.10) sin cambiar el orden del filtro ni el comportamiento; el warning `[removal]` debe desaparecer.
8. Verificar `./gradlew test jacocoTestReport jacocoTestCoverageVerification` y `./gradlew shadowJar`.

## Fuera de alcance
Push a GitHub, cambios de lógica de negocio, specs ya cerradas, borrados no autorizados.

## Criterios de aceptación
1. El README no contiene rutas ni supuestos específicos de esta máquina y describe el estado real.
2. `NOTES.md`/`MEMORY.md` listan los 11 commits reales; no queda "commit pendiente" vigente.
3. `build.gradle.kts` no incluye H2; los 55 tests y la cobertura ≥70% siguen pasando.
4. Existe `LICENSE` MIT y el README lo refleja; no hay archivos obsoletos borrados sin registro.
5. `V3__AddForeignKeys.sql` aplica las 3 FK con 0 huérfanos; se documentan las filas borradas y las conservadas.
6. `RateLimitFilter` compila sin warnings `[removal]` y mantiene los 429 (tests incluidos).

## Comandos de verificación
`podman exec bank-api-db psql -U bank_user -d bank_api -c '\dt'` · `./gradlew test jacocoTestReport jacocoTestCoverageVerification` · `./gradlew shadowJar`

## Definition of Done
Reportar archivos creados/modificados/borrados, filas de BD borradas y conservadas, salida literal de los comandos, incidencias/desviaciones y checklist de los 6 criterios. **No hacer commit ni push.**
