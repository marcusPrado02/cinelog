package com.cine.cinelog.shared.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Aspecto para logging de chamadas a endpoints REST.
 * 
 * Registra entrada, saída e erros lançados pelos endpoints.
 */
@Aspect
@Component
public class WebLoggingAspect {
    private static final Logger log = LoggerFactory.getLogger(WebLoggingAspect.class);

    /**
     * Aspecto ao redor de controladores REST.
     * 
     * Registra logs de entrada e saída, incluindo tratamento de exceções de
     * domínio.
     * 
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("within(@org.springframework.web.bind.annotation.RestController *)")
    public Object aroundController(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        String traceId = MDC.get("trace_id");
        String spanId = MDC.get("span_id");
        String signature = proceedingJoinPoint.getSignature().toShortString();
        String args = LogSanitizer.sanitize(java.util.Arrays.toString(proceedingJoinPoint.getArgs()));

        long start = System.nanoTime();
        log.info(" web_start traceId={} spanId={} endpoint={} args={}", traceId, spanId, signature, args);

        try {
            Object result = proceedingJoinPoint.proceed();
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.info(" web_success traceId={} spanId={} endpoint={} tookMs={} result={}",
                    traceId, spanId, signature, tookMs, LogSanitizer.sanitize(String.valueOf(result)));
            return result;
        } catch (Exception e) {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.error(" web_error traceId={} spanId={} endpoint={} tookMs={} errorClass={} msg={}",
                    traceId, spanId, signature, tookMs, e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Sanitiza strings para logging, removendo dados sensíveis.
     */
    final class LogSanitizer {
        static String sanitize(String s) {
            if (s == null)
                return "null";
            // regras simples: mascara emails e tokens
            s = s.replaceAll("([a-zA-Z0-9._%+-]+)@([a-zA-Z0-9.-]+)", "***@***");
            s = s.replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]+\"", "\"password\":\"***\"");
            s = s.replaceAll("(?i)authorization: Bearer [A-Za-z0-9._-]+", "authorization: Bearer ***");
            // limita tamanho
            int max = 2000;
            return s.length() > max ? s.substring(0, max) + "...<truncated>" : s;
        }
    }

}