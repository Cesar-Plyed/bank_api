# Spec-08 — Suite completa y cobertura

## Objetivo
Completar la suite con tests de servicio e integración de extremo a extremo, y medir la cobertura con JaCoCo.

## Alcance
- JaCoCo: plugin, `jacocoTestReport` (HTML + XML) y `jacocoTestCoverageVerification` con mínimo del **70% de líneas**. Si no se alcanza, se añaden tests; no se baja el umbral.
- Tests de servicio: `AccountServiceTest` y `TransactionServiceTest` sobre la BD real de Podman (operaciones normales y excepciones de dominio).
- `BankApiIntegrationTest`: flujo completo `register` → `login` → crear dos cuentas → depósito → retiro → transferencia → historial → borrar cuenta con saldo 0, verificando balances, statuses y payloads.
- `src/test/resources/application-test.yml` mantiene `ratelimit.enabled=false` para que el flujo E2E no reciba 429.
- Documentar `./gradlew test jacocoTestReport jacocoTestCoverageVerification`, el reporte en `build/reports/jacoco/test/html/index.html` y la desviación (Testcontainers no usado: se comparte el contenedor Podman; REST Assured no usado: se usa `Micronaut HttpClient`).
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md` con el número de tests y la cobertura real.

## Fuera de alcance
Empaquetado Podman/Docker (spec-09), cierre documental (spec-10), commits.

## Criterios de aceptación
1. `./gradlew test jacocoTestReport jacocoTestCoverageVerification` pasa y genera HTML y XML.
2. Cobertura de líneas ≥ 70% para `io.onstructive.micronaut.*`.
3. La suite tiene al menos 45 tests, 0 failures/errors; los 37 existentes siguen pasando.
4. El test E2E verifica balances y todos los endpoints del alcance del README (auth, cuentas, transacciones, historial).
5. Notas actualizadas con tests, cobertura y desviaciones.

## Comandos de verificación
`./gradlew test jacocoTestReport jacocoTestCoverageVerification` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, porcentaje de cobertura, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
