package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEvent;
import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Rate Limiting por IP usando algoritmo de janela fixa (Fixed Window)
 * com contadores persistidos no Redis.
 *
 * <h3>A04 (OWASP) — Insecure Design: por que Rate Limiting é questão de design?</h3>
 * <p>
 * Sem rate limiting, um sistema está <b>arquiteturalmente</b> vulnerável a abuso,
 * independente de quão bem o código está escrito. Mesmo que cada endpoint tenha
 * autenticação, validação e prepared statements perfeitos, um atacante pode:
 * </p>
 * <ul>
 * <li><b>Brute force</b>: testar milhares de senhas/segundo no endpoint de login</li>
 * <li><b>Credential stuffing</b>: testar credenciais vazadas de outros serviços</li>
 * <li><b>DoS (Denial of Service)</b>: sobrecarregar o servidor com requests</li>
 * <li><b>Scraping</b>: extrair todos os dados da API programaticamente</li>
 * </ul>
 *
 * <h3>Algoritmo: Fixed Window com Redis</h3>
 * <p>
 * Divide o tempo em janelas fixas (60 segundos). Cada IP recebe uma chave Redis
 * com TTL = janela. O contador é incrementado atomicamente via script Lua.
 * </p>
 *
 * <p>
 * <b>Multi-instância:</b> os contadores são compartilhados entre todas as
 * instâncias via Redis — o rate limit funciona corretamente em escala horizontal.
 * </p>
 *
 * <p>
 * <b>Fallback:</b> se o Redis estiver indisponível, o filtro recai em contadores
 * in-memory por instância. Isso preserva disponibilidade às custas de enforcement
 * menos preciso durante falhas do Redis.
 * </p>
 *
 * @since 1.2 (Redis-backed)
 * @see InputSanitizer
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    /** Limite padrão: 100 requisições por janela. */
    private static final int DEFAULT_LIMIT = 100;

    /** Limite reduzido para endpoints de autenticação (anti brute force). */
    private static final int AUTH_LIMIT = 10;

    /** Duração da janela em segundos. */
    private static final long WINDOW_SECONDS = 60;

    /** Prefixo das chaves Redis para isolamento do namespace. */
    private static final String KEY_PREFIX = "ratelimit:";

    /**
     * Script Lua para incremento atômico com TTL.
     *
     * <p>
     * Atomicidade garante que INCR e EXPIRE sejam executados como operação única,
     * sem race condition entre múltiplas instâncias.
     * </p>
     *
     * <pre>
     * KEYS[1] = chave Redis (ex: ratelimit:192.168.1.1:auth)
     * ARGV[1] = duração da janela em segundos
     * Retorna: contagem atual após incremento
     * </pre>
     */
    private static final DefaultRedisScript<Long> INCR_SCRIPT;

    static {
        INCR_SCRIPT = new DefaultRedisScript<>();
        INCR_SCRIPT.setScriptText(
                "local current = redis.call('INCR', KEYS[1]) " +
                "if current == 1 then " +
                "  redis.call('EXPIRE', KEYS[1], ARGV[1]) " +
                "end " +
                "return current");
        INCR_SCRIPT.setResultType(Long.class);
    }

    /** A09:2025 — Logger de eventos de segurança. */
    private final SecurityEventLogger securityEventLogger;

    /** A09:2025 — Métricas de segurança. */
    private final SecurityMetricsService securityMetrics;

    /** Redis template para contadores distribuídos. */
    private final StringRedisTemplate redisTemplate;

    /**
     * Fallback in-memory para quando Redis estiver indisponível.
     * Garante continuidade do serviço — rate limit pode ser menos preciso durante
     * falhas do Redis.
     */
    private final Map<String, WindowCounter> fallbackCounters = new ConcurrentHashMap<>();

    public RateLimitFilter(SecurityEventLogger securityEventLogger,
            SecurityMetricsService securityMetrics,
            StringRedisTemplate redisTemplate) {
        this.securityEventLogger = securityEventLogger;
        this.securityMetrics = securityMetrics;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String bucket = classifyPath(path);
        String redisKey = KEY_PREFIX + clientIp + ":" + bucket;
        int limit = getLimit(path);

        RateLimitResult result = increment(redisKey);
        int currentCount = result.count();
        long resetEpoch = result.resetEpoch();

        // Headers informativos (RFC 6585 / draft-ietf-httpapi-ratelimit-headers)
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetEpoch));

        if (currentCount > limit) {
            log.warn("A04-RateLimit: Limite excedido — IP={}, path={}, count={}/{}",
                    InputSanitizer.sanitizeForLog(clientIp),
                    InputSanitizer.sanitizeForLog(path),
                    currentCount, limit);

            // A09:2025 — Evento de segurança + métrica dedicada
            securityEventLogger.log(SecurityEvent.RATE_LIMITED, Map.of(
                    "ip", InputSanitizer.sanitizeForLog(clientIp),
                    "path", InputSanitizer.sanitizeForLog(path),
                    "count", currentCount,
                    "limit", limit));
            securityMetrics.incrementRateLimit(bucket);

            long retryAfter = Math.max(0, resetEpoch - Instant.now().getEpochSecond());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(
                    "{\"type\":\"about:blank\"," +
                    "\"title\":\"Too Many Requests\"," +
                    "\"status\":429," +
                    "\"detail\":\"Limite de requisições excedido. Tente novamente em breve.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Incrementa o contador no Redis. Em caso de falha, usa o fallback in-memory.
     *
     * @param key chave Redis do contador
     * @return {@link RateLimitResult} com contagem atual e epoch de reset
     */
    private RateLimitResult increment(String key) {
        try {
            Long count = redisTemplate.execute(
                    INCR_SCRIPT,
                    List.of(key),
                    String.valueOf(WINDOW_SECONDS));

            long currentCount = (count != null) ? count : 1L;
            Long ttl = redisTemplate.getExpire(key);
            long secondsUntilReset = (ttl != null && ttl > 0) ? ttl : WINDOW_SECONDS;
            long resetEpoch = Instant.now().getEpochSecond() + secondsUntilReset;

            return new RateLimitResult((int) currentCount, resetEpoch);

        } catch (Exception ex) {
            log.warn("Redis rate limit indisponível, usando fallback in-memory: {}", ex.getMessage());
            WindowCounter counter = fallbackCounters.computeIfAbsent(key, k -> new WindowCounter());
            int count = counter.incrementAndGet(WINDOW_SECONDS);
            long resetEpoch = counter.getWindowResetEpoch(WINDOW_SECONDS);
            return new RateLimitResult(count, resetEpoch);
        }
    }

    /**
     * Resolve o IP real do cliente, considerando proxies reversos.
     *
     * <p><b>Ordem de prioridade:</b></p>
     * <ol>
     * <li>{@code X-Forwarded-For} — padrão de proxies (Nginx, ALB, CloudFlare)</li>
     * <li>{@code X-Real-IP} — configuração alternativa do Nginx</li>
     * <li>{@code request.getRemoteAddr()} — fallback (IP direto)</li>
     * </ol>
     *
     * <p>
     * <b>⚠️ Segurança:</b> o header {@code X-Forwarded-For} pode ser falsificado
     * pelo cliente. Em produção, o proxy deve sobrescrever esse header:
     * {@code proxy_set_header X-Forwarded-For $remote_addr;}
     * </p>
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Classifica o path para buckets separados de rate limit.
     * Endpoints de auth têm bucket próprio com limite menor.
     */
    private String classifyPath(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return "auth";
        }
        return "general";
    }

    /**
     * Define limites diferentes por tipo de endpoint.
     *
     * <p>
     * <b>Por que auth tem limite menor?</b> Endpoints de login/registro são os
     * principais alvos de brute force. Um humano real não tenta login 10 vezes
     * por minuto — se está tentando, é ataque.
     * </p>
     */
    private int getLimit(String path) {
        if (path.startsWith("/api/v1/auth")) {
            return AUTH_LIMIT;
        }
        return DEFAULT_LIMIT;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator/health");
    }

    /**
     * Resultado de uma operação de rate limit.
     *
     * @param count      contagem atual nesta janela
     * @param resetEpoch epoch Unix em que a janela atual expira
     */
    record RateLimitResult(int count, long resetEpoch) {
    }

    /**
     * Contador in-memory de fallback. Usado apenas quando Redis está indisponível.
     *
     * <p><b>Thread-safety:</b> usa {@link AtomicInteger} para o contador e
     * {@code synchronized} para reset da janela (operação composta).</p>
     */
    static class WindowCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile long windowStart = Instant.now().getEpochSecond();

        synchronized int incrementAndGet(long windowSeconds) {
            long now = Instant.now().getEpochSecond();
            if (now - windowStart >= windowSeconds) {
                count.set(0);
                windowStart = now;
            }
            return count.incrementAndGet();
        }

        long getWindowResetEpoch(long windowSeconds) {
            return windowStart + windowSeconds;
        }

        long getSecondsUntilReset(long windowSeconds) {
            long remaining = (windowStart + windowSeconds) - Instant.now().getEpochSecond();
            return Math.max(0, remaining);
        }
    }
}
