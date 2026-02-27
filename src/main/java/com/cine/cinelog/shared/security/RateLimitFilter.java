package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEvent;
import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Filtro de Rate Limiting por IP usando algoritmo de janela fixa (Fixed
 * Window).
 *
 * <h3>A04 (OWASP) — Insecure Design: por que Rate Limiting é questão de
 * design?</h3>
 * <p>
 * Sem rate limiting, um sistema está <b>arquiteturalmente</b> vulnerável a
 * abuso,
 * independente de quão bem o código está escrito. Mesmo que cada endpoint tenha
 * autenticação, validação e prepared statements perfeitos, um atacante pode:
 * </p>
 * <ul>
 * <li><b>Brute force</b>: testar milhares de senhas/segundo no endpoint de
 * login</li>
 * <li><b>Credential stuffing</b>: testar credenciais vazadas de outros
 * serviços</li>
 * <li><b>DoS (Denial of Service)</b>: sobrecarregar o servidor com requests
 * legítimas</li>
 * <li><b>Scraping</b>: extrair todos os dados da API programaticamente</li>
 * </ul>
 *
 * <h3>Algoritmo: Fixed Window</h3>
 * <p>
 * Divide o tempo em janelas fixas (ex: 60 segundos). Cada IP recebe um contador
 * por janela. Quando a janela expira, o contador reseta.
 * </p>
 *
 * <p>
 * <b>Vantagens:</b> simples, baixo consumo de memória, fácil de entender.<br/>
 * <b>Desvantagem:</b> na fronteira entre duas janelas, um cliente pode alcançar
 * até 2x o limite (ex: 100 no final da janela 1 + 100 no início da janela 2).
 * Em produção, considerar Sliding Window ou Token Bucket.
 * </p>
 *
 * <h3>Por que por IP?</h3>
 * <p>
 * É a forma mais básica de identificação pré-autenticação. Em produção,
 * pode ser combinado com rate limit por usuário autenticado e por API key.
 * </p>
 *
 * <p>
 * <b>Nota:</b> em ambientes com múltiplas instâncias, esta implementação
 * in-memory deve ser substituída por Redis (chave = IP, valor = contador,
 * TTL = janela) para que o rate limit seja compartilhado entre instâncias.
 * </p>
 *
 * @since 1.1
 * @see InputSanitizer
 * @see SqlInjectionFilter
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

    /**
     * Contadores por IP. Em produção com múltiplas instâncias,
     * seria substituído por Redis (chave = IP, valor = contador, TTL = janela).
     */
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    /** A09:2025 — Logger de eventos de segurança. */
    private final SecurityEventLogger securityEventLogger;

    /** A09:2025 — Métricas de segurança. */
    private final SecurityMetricsService securityMetrics;

    public RateLimitFilter(SecurityEventLogger securityEventLogger,
            SecurityMetricsService securityMetrics) {
        this.securityEventLogger = securityEventLogger;
        this.securityMetrics = securityMetrics;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String path = request.getRequestURI();
        String key = clientIp + ":" + classifyPath(path);
        int limit = getLimit(path);

        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());
        int currentCount = counter.incrementAndGet(WINDOW_SECONDS);

        // Headers informativos (RFC 6585 / draft-ietf-httpapi-ratelimit-headers)
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(counter.getWindowResetEpoch(WINDOW_SECONDS)));

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
            securityMetrics.incrementRateLimit(classifyPath(path));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After",
                    String.valueOf(counter.getSecondsUntilReset(WINDOW_SECONDS)));
            response.getWriter().write(
                    "{\"type\":\"about:blank\","
                            + "\"title\":\"Too Many Requests\","
                            + "\"status\":429,"
                            + "\"detail\":\"Limite de requisições excedido. Tente novamente em breve.\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve o IP real do cliente, considerando proxies reversos.
     *
     * <p>
     * <b>Ordem de prioridade:</b>
     * </p>
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
     * Classifica o path para agrupar rate limits em buckets separados.
     * Endpoints de auth têm bucket próprio com limite menor.
     */
    private String classifyPath(String path) {
        if (path.startsWith("/api/auth")) {
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
        if (path.startsWith("/api/auth")) {
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
     * Contador com janela temporal fixa (Fixed Window).
     *
     * <p>
     * <b>Thread-safety:</b> usa {@link AtomicInteger} para o contador e
     * {@code synchronized} para o reset da janela (operação composta).
     * </p>
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
