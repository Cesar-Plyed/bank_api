# Spec-06 — Observabilidad: Swagger, health y logs estructurados

## Objetivo
Documentación OpenAPI accesible, endpoints de salud verificables y logs en formato JSON.

## Alcance
- Anotaciones OpenAPI en `AuthController` para que registro/login aparezcan en el spec generado; verificar que el YAML de `build/classes/java/main/META-INF/swagger/` contenga todas las rutas `/api/**` (auth, cuentas, transacciones, historial).
- Swagger UI anónimo en `/swagger-ui.html` (añadir un `intercept-url-map` explícito si hace falta) y spec servido desde `/swagger/**`.
- Añadir `micronaut-management` y configurar `endpoints.all.path=/actuator`, `endpoints.health.enabled=true`, `endpoints.health.details.enabled=true` para que `/actuator/health` informe `database` y `diskSpace`.
- `HealthController` público en `/health` que devuelva `{"status":"UP"}`.
- Logs estructurados: encoder JSON de Logstash en `logback.xml` (añadir la dependencia `logstash-logback-encoder`).
- Tests: `/health` y `/actuator/health` → 200 con `status=UP` y detalles de DB/disco; `/swagger-ui.html` → 200 sin token; el OpenAPI generado contiene las rutas; una línea de log es JSON válido con `timestamp`, `level`, `logger` y `message`.
- Actualizar `AGENTS.md`, `NOTES.md` y `MEMORY.md` con las rutas de salud/docs y el formato de log.

## Fuera de alcance
Rate limiting (spec-07), suite completa y cobertura (spec-08), empaquetado (spec-09), commits.

## Criterios de aceptación
1. `/health` y `/actuator/health` responden 200 con `status=UP` y detalles `database`/`diskSpace`.
2. `/swagger-ui.html` responde 200 sin token y el OpenAPI generado contiene todas las rutas `/api/**`.
3. Los logs son JSON válido por línea con `timestamp`, `level`, `logger` y `message`.
4. `./gradlew test` pasa con los 27 tests existentes y los nuevos: 0 failures/errors.
5. Notas actualizadas con salud, Swagger y logs.

## Comandos de verificación
`./gradlew test --tests "*Health*Test"` · `./gradlew test`

## Definition of Done
Reportar archivos, salida literal de los comandos, incidencias/desviaciones y checklist de los 5 criterios. **No hacer commit.**
