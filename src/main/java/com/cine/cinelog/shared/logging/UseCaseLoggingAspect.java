package com.cine.cinelog.shared.logging;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
// imports novos
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import io.opentelemetry.api.trace.Span;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Aspecto para logging de chamadas a casos de uso (use cases).
 * 
 * Registra entrada, saída, duração e erros lançados pelos casos de uso.
 * Executa depois de filtros web, antes de repository aspects (se houver)
 */
@Aspect
@Configuration
@Order(10)
public class UseCaseLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(UseCaseLoggingAspect.class);

    @Value("${logging.usecase.slow-threshold-ms:1000}")
    long slowThresholdMs;

    @Value("${logging.usecase.sample-rate:1.0}")
    double sampleRate;

    @Pointcut("execution(public * com.cine.cinelog.core.application.usecase..*(..))")
    void usecaseOps() {
    }

    /**
     * Aspecto ao redor de casos de uso.
     * 
     * @param proceedingJoinPoint
     * @return
     * @throws Throwable
     */
    @Around("usecaseOps()")
    public Object aroundUseCase(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        if (sampleRate < 1.0 && Math.random() > sampleRate) {
            return proceedingJoinPoint.proceed();
        }

        long start = System.nanoTime();
        String signature = proceedingJoinPoint.getSignature().toShortString();
        String traceId = MDC.get("trace_id");
        String spanId = MDC.get("span_id");

        log.info("▶️ usecase_start traceId={} spanId={} method={} args={}",
                traceId, spanId, signature, safeArgs(proceedingJoinPoint.getArgs()));

        try {
            Object result = proceedingJoinPoint.proceed();
            long tookMs = (System.nanoTime() - start) / 1_000_000;

            Span.current().setAttribute("usecase.name", signature);
            Span.current().setAttribute("usecase.duration.ms", tookMs);

            if (tookMs >= slowThresholdMs) {
                log.warn("usecase_slow traceId={} spanId={} method={} tookMs={}",
                        traceId, spanId, signature, tookMs);
            }

            log.info("usecase_success traceId={} spanId={} method={} tookMs={} result={}",
                    traceId, spanId, signature, tookMs, safe(result));
            return result;
        } catch (Exception ex) {
            long tookMs = (System.nanoTime() - start) / 1_000_000;
            log.error("usecase_error traceId={} spanId={} method={} tookMs={} errorClass={} msg={}",
                    traceId, spanId, signature, tookMs, ex.getClass().getSimpleName(), ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Segurança ao converter objeto para string.
     * 
     * @param o
     * @return
     */
    private Object safe(Object o) {
        try {
            return o != null ? o.toString() : "null";
        } catch (Exception e) {
            return "<unprintable>";
        }
    }

    /**
     * Segurança ao converter argumentos para string.
     * 
     * @param args
     * @return
     */
    private Object safeArgs(Object[] args) {
        try {
            return java.util.Arrays.toString(args);
        } catch (Exception e) {
            return "<args_unprintable>";
        }
    }
}
