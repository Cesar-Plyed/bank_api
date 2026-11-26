package io.onstructive.micronaut.filter;

import io.micronaut.context.annotation.Value;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpAttributes;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import io.micronaut.http.filter.ServerFilterPhase;
import io.micronaut.security.authentication.Authentication;
import io.onstructive.micronaut.exception.ErrorPayload;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.reactivestreams.Publisher;

/**
 * Rate limiting de {@code /api/**} (spec-07): contadores en memoria por ventana
 * fija para la IP (minuto), el usuario autenticado (hora) y las peticiones de
 * transacciones (minuto): tanto {@code /api/transactions/**} como el historial
 * {@code GET /api/accounts/{accountId}/transactions}. Ambas rutas comparten el
 * mismo bucket (de IP o de usuario), por lo que suman sus peticiones al límite.
 * Al superar cualquiera de los límites se responde 429 con el payload único de
 * errores y el código {@code RATE_LIMIT_EXCEEDED}.
 *
 * <p>Se ejecuta justo después del filtro de seguridad para poder leer el
 * {@link Authentication} ya resuelto y atribuir las peticiones al usuario real
 * del JWT. Los contadores son locales a la instancia: se reinician al arrancar
 * y no se comparten entre réplicas (limitación documentada).
 */
@Filter("/api/**")
public class RateLimitFilter implements HttpServerFilter {

    static final String MESSAGE = "Rate limit exceeded";
    static final String TRANSACTIONS_PREFIX = "/api/transactions/";
    static final String ACCOUNTS_PREFIX = "/api/accounts/";
    static final String HISTORY_SUFFIX = "/transactions";

    private final boolean enabled;
    private final FixedWindowLimiter perIp;
    private final FixedWindowLimiter perUser;
    private final FixedWindowLimiter transactions;

    public RateLimitFilter(
            @Value("${ratelimit.enabled:true}") boolean enabled,
            @Value("${ratelimit.per-ip.minute:100}") int perIpMinute,
            @Value("${ratelimit.per-user.hour:1000}") int perUserHour,
            @Value("${ratelimit.transactions.minute:10}") int transactionsMinute) {
        this.enabled = enabled;
        this.perIp = new FixedWindowLimiter(perIpMinute, 60_000L);
        this.perUser = new FixedWindowLimiter(perUserHour, 3_600_000L);
        this.transactions = new FixedWindowLimiter(transactionsMinute, 60_000L);
    }

    @Override
    public int getOrder() {
        // Después de SecurityFilter (39000): el principal ya está en el request.
        return ServerFilterPhase.SECURITY.after();
    }

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        if (!enabled) {
            return chain.proceed(request);
        }

        String ip = clientIp(request);
        String user = authenticatedUser(request);
        String path = request.getPath();

        if (!perIp.allow(ip)) {
            return reject(request);
        }
        if (user != null && !perUser.allow(user)) {
            return reject(request);
        }
        if (isTransactionPath(path)) {
            // Mismo bucket para /api/transactions/** y para el historial
            // /api/accounts/{accountId}/transactions: ambas suman al límite.
            String key = user != null ? "user:" + user : "ip:" + ip;
            if (!transactions.allow(key)) {
                return reject(request);
            }
        }
        return chain.proceed(request);
    }

    /**
     * {@code true} para las escrituras de transacciones y para el historial de
     * una cuenta ({@code /api/accounts/{accountId}/transactions}, spec-04).
     */
    private boolean isTransactionPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith(TRANSACTIONS_PREFIX)
                || (path.startsWith(ACCOUNTS_PREFIX) && path.endsWith(HISTORY_SUFFIX));
    }

    /** El usuario autenticado (nombre del JWT), o {@code null} si es anónimo. */
    private String authenticatedUser(HttpRequest<?> request) {
        return request.getAttribute(HttpAttributes.PRINCIPAL.toString(), Authentication.class)
                .map(Authentication::getName)
                .orElse(null);
    }

    /**
     * IP del cliente. Se prefiere {@code X-Forwarded-For} (cabecera estándar
     * tras un proxy) y se cae a la dirección remota del socket.
     */
    private String clientIp(HttpRequest<?> request) {
        return request.getHeaders().get("X-Forwarded-For", String.class)
                .map(value -> value.split(",", -1)[0].trim())
                .filter(value -> !value.isEmpty())
                .orElseGet(() -> Optional.ofNullable(request.getRemoteAddress())
                        .map(address -> address.getAddress() != null
                                ? address.getAddress().getHostAddress()
                                : address.getHostString())
                        .orElse("unknown"));
    }

    private Publisher<MutableHttpResponse<?>> reject(HttpRequest<?> request) {
        return Publishers.just(HttpResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ErrorPayload.of(ErrorPayload.RATE_LIMIT_EXCEEDED, MESSAGE, request.getPath(), null)));
    }

    /** Contador con ventana fija y reinicio automático al cambiar de ventana. */
    private static final class FixedWindowLimiter {
        private final int limit;
        private final long windowMillis;
        private final ConcurrentMap<String, Counter> counters = new ConcurrentHashMap<>();

        private FixedWindowLimiter(int limit, long windowMillis) {
            this.limit = limit;
            this.windowMillis = windowMillis;
        }

        private boolean allow(String key) {
            long window = System.currentTimeMillis() / windowMillis;
            Counter counter = counters.computeIfAbsent(key, ignored -> new Counter());
            return counter.allow(window, limit);
        }
    }

    private static final class Counter {
        private long window = -1L;
        private int count;

        private synchronized boolean allow(long window, int limit) {
            if (this.window != window) {
                this.window = window;
                this.count = 0;
            }
            this.count++;
            return this.count <= limit;
        }
    }
}
