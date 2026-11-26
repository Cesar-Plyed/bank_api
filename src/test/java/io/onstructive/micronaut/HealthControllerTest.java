package io.onstructive.micronaut;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.encoder.Encoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import net.logstash.logback.encoder.LogstashEncoder;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Observabilidad (spec-06): health, Swagger/OpenAPI y logs JSON. */
@MicronautTest(transactional = false)
class HealthControllerTest {

    private static final List<String> EXPECTED_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/api/accounts",
            "/api/accounts/{id}",
            "/api/transactions/deposit",
            "/api/transactions/withdraw",
            "/api/transactions/transfer",
            "/api/accounts/{accountId}/transactions");

    @Inject
    @Client("/")
    HttpClient client;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** GET anonimo que devuelve un cuerpo JSON ya parseado. */
    private Map<?, ?> getJson(String uri) throws Exception {
        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String.class);
        assertEquals(200, response.code(), "GET " + uri + " should be 200 without a token");
        assertNotNull(response.body());
        return objectMapper.readValue(response.body(), Map.class);
    }

    /** Busca una clave en un mapa anidado (los detalles de health lo estan). */
    private static boolean containsKeyDeep(Map<?, ?> map, String key) {
        if (map.containsKey(key)) {
            return true;
        }
        for (Object value : map.values()) {
            if (value instanceof Map<?, ?> nested && containsKeyDeep(nested, key)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void healthReturnsUpWithoutToken() throws Exception {
        Map<?, ?> body = getJson("/health");
        assertEquals("UP", body.get("status"));
    }

    @Test
    void actuatorHealthReturnsUpWithDatabaseAndDiskSpaceDetails() throws Exception {
        Map<?, ?> body = getJson("/actuator/health");
        assertEquals("UP", body.get("status"));

        Object detailsValue = body.get("details");
        assertTrue(detailsValue instanceof Map<?, ?>, "actuator health must expose details");
        Map<?, ?> details = (Map<?, ?>) detailsValue;

        Object diskSpace = details.get("diskSpace");
        assertTrue(diskSpace instanceof Map<?, ?>, "diskSpace detail is required");
        assertEquals("UP", ((Map<?, ?>) diskSpace).get("status"));

        // Micronaut Management nombra el indicador JDBC "jdbc" (no "database"),
        // y dentro de sus detalles incluye "database" = PostgreSQL.
        assertTrue(details.get("jdbc") instanceof Map<?, ?>, "JDBC/database detail is required");
        assertTrue(containsKeyDeep(details, "database"),
                "database details must be present (JDBC indicator)");
    }

    @Test
    void swaggerUiIsReachableAnonymously() throws Exception {
        HttpResponse<String> response = client.toBlocking()
                .exchange(HttpRequest.GET("/swagger-ui.html"), String.class);
        assertEquals(200, response.code(), "/swagger-ui.html must be public");
        assertTrue(response.body().toLowerCase().contains("swagger-ui"),
                "should serve the Swagger UI page");
    }

    @Test
    void generatedOpenApiContainsAllApiRoutes() throws Exception {
        Path yaml = Path.of("build/classes/java/main/META-INF/swagger/bank-account-api-1.0.yml");
        assertTrue(Files.exists(yaml), "generated OpenAPI YAML must exist: " + yaml);
        String spec = Files.readString(yaml);
        for (String path : EXPECTED_PATHS) {
            assertTrue(spec.contains("  " + path + ":"), "OpenAPI must declare " + path);
        }
        assertTrue(spec.contains("Auth"), "AuthController must be annotated/tagged for OpenAPI");
    }

    @Test
    void logLineIsValidJsonWithRequiredFields() throws Exception {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        ch.qos.logback.classic.Logger root = context.getLogger(
                ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        ConsoleAppender<?> appender = (ConsoleAppender<?>) root.getAppender("STDOUT");
        assertNotNull(appender, "logback.xml must define the STDOUT appender");

        Encoder<?> encoder = appender.getEncoder();
        assertTrue(encoder instanceof LogstashEncoder, "the STDOUT appender must use the Logstash JSON encoder");

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setTimeStamp(System.currentTimeMillis());
        event.setLevel(Level.INFO);
        event.setLoggerName("io.onstructive.micronaut.HealthControllerTest");
        event.setThreadName("spec-06");
        event.setMessage("observability log line");
        event.setArgumentArray(new Object[0]);

        String line = new String(((Encoder<LoggingEvent>) encoder).encode(event), StandardCharsets.UTF_8).trim();
        assertTrue(line.startsWith("{") && line.endsWith("}"), "log line must be a single JSON object: " + line);

        Map<?, ?> json = objectMapper.readValue(line, Map.class);
        assertTrue(json.containsKey("timestamp"), "timestamp field is required");
        assertEquals("INFO", json.get("level"));
        assertEquals("io.onstructive.micronaut.HealthControllerTest", json.get("logger"));
        assertEquals("observability log line", json.get("message"));
    }
}
