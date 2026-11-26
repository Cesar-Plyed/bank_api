# Spec-07 — Rate limiting

## Objetivo
Limitar peticiones por IP, por usuario y de transacciones, devolviendo 429 con el payload de errores único.

## Alcance
- Propiedades configurables con los valores del README: `ratelimit.enabled=true`, `ratelimit.per-ip.minute=100`, `ratelimit.per-user.hour=1000`, `ratelimit.transactions.minute=10`.
- `RateLimitFilter` (`HttpFilter` sobre `/api/**`) con contadores en memoria por ventana fija: IP (minuto), usuario autenticado (hora) y peticiones a `/api/transactions/**` (minuto). Al excederse → 429 con `ErrorPayload` y código `RATE_LIMIT_EXCEEDED`.
- `src/test/resources/application-test.yml` con `ratelimit.enabled=false` para no alterar los 32 tests existentes; `RateLimitTest` lo activa con `@Property` y límites pequeños.
- Tests: 429 por IP, por usuario y por transacciones (con el payload único), y ausencia de 429 cuando `enabled=false`.
- Documentar límites, configuración y la desviación (en memoria, por instancia, se reinicia al arrancar) en `AGENTS.md`, `NOTES.md` y `MEMORY.md`.

## Fuera de alcance
Suite completa y cobertura (spec-08), empaquetado (spec-09), cierre documental (spec-10), commits.

## Criterios de aceptación
1. Los defaults son los del README y `ratelimit.enabled=false` desactiva el filtro.
2. Los tres límites devuelven 429 con `{ "error": "RATE_LIMIT_EXCEEDED", "message", "path", "timestamp" }`.
3. Los 32 tests existentes siguen pasando y se añaden al menos 3 nuevos.
4. `./gradlew test` pasa: 0 failures/errors.
5. Notas actualizadas con límites y desviación.

## Comandos de verificación
`./gradlew test --tests "*RateLimit*Test"` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
