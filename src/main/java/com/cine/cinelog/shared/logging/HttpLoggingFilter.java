package com.cine.cinelog.shared.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Filtro HTTP para log estruturado de requisições/respostas.
 * - Gera request_id (MDC) para correlação.
 * - Mascara dados sensíveis em headers/conteúdos logados.
 * - Registra método, path, status, tempo, tamanho e user-agent.
 */
@Component
@Order(1)
public class HttpLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingFilter.class);

    @Value("${logging.http.max-len:2000}")
    private int maxLen;

    /**
     * Filtro HTTP para log estruturado de requisições/respostas.
     * 
     * @param request
     * @param response
     * @param chain
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // request id para correlacionar logs do mesmo ciclo
        String requestId = Optional.ofNullable(MDC.get("request_id"))
                .orElse(UUID.randomUUID().toString());
        MDC.put("request_id", requestId);

        String method = request.getMethod();
        String path = request.getRequestURI();
        String query = request.getQueryString();
        String ua = mask(piSafe(request.getHeader("User-Agent")));
        String ip = clientIp(request);

        long startNs = System.nanoTime();

        log.info("http_start requestId={} ip={} method={} path={}{} ua={}",
                requestId, ip, method, path, (query == null ? "" : "?" + query), ua);

        ContentCachingResponseWrapper respWrapper = new ContentCachingResponseWrapper(response);
        try {
            chain.doFilter(request, respWrapper);
        } catch (Exception ex) {
            long tookMs = tookMs(startNs);
            log.error(" http_error requestId={} ip={} method={} path={} status={} tookMs={} errorClass={} msg={}",
                    requestId, ip, method, path, respWrapper.getStatus(), tookMs,
                    ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        } finally {
            long tookMs = tookMs(startNs);
            int status = respWrapper.getStatus();
            int size = respWrapper.getContentSize();
            log.info("http_end requestId={} ip={} method={} path={} status={} tookMs={} sizeBytes={}",
                    requestId, ip, method, path, status, tookMs, size);
            respWrapper.copyBodyToResponse();
            // limpa MDC criado aqui
            MDC.remove("request_id");
        }
    }

    private long tookMs(long startNs) {
        return (System.nanoTime() - startNs) / 1_000_000;
    }

    /** Retorna IP do cliente, respeitando proxies. */
    private String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // pode vir em lista: pega o primeiro
            int comma = xff.indexOf(',');
            return comma > 0 ? xff.substring(0, comma).trim() : xff.trim();
        }
        String realIp = req.getHeader("X-Real-IP");
        return realIp != null ? realIp : req.getRemoteAddr();
    }

    /**
     * Sanitiza strings para log (mascara e trunca).
     */
    private String mask(String s) {
        if (s == null)
            return "null";
        String v = s;

        // Bearer / tokens
        v = v.replaceAll("(?i)\\bauthorization:\\s*Bearer\\s+[A-Za-z0-9._\\-+=/]+", "authorization: Bearer ***");
        v = v.replaceAll("(?i)Bearer\\s+[A-Za-z0-9._\\-+=/]+", "Bearer ***");

        // emails
        v = v.replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)", "***@***");

        // password em JSON
        v = v.replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"***\"");

        // truncamento
        if (v.length() > maxLen) {
            v = v.substring(0, maxLen) + "...<truncated>";
        }
        return v;
    }

    /**
     * Proteção de informações (PI) em strings para log.
     * 
     * @param s
     * @return
     */
    private String piSafe(String s) {
        return mask(s);
    }
}
