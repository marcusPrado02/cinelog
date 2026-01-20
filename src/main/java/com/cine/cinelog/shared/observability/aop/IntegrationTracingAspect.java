package com.cine.cinelog.shared.observability.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Aspect responsável por criar spans e métricas de integração para chamadas
 * externas,
 * como TMDb e outros serviços de terceiros.
 *
 * Pointcut:
 * - Métodos públicos em packages de integração:
 * com.cine.cinelog.features..integration..
 *
 * Métrica:
 * - cinelog.integration.duration
 * Tags:
 * - service (tmdb, unknown), class, method, outcome (SUCCESS/ERROR)
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnBean(Tracer.class)
@ConditionalOnProperty(prefix = "cinelog.integration.tracing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationTracingAspect {

    private final Tracer tracer;
    private final MeterRegistry meterRegistry;

    @Around("execution(public * com.cine.cinelog.features..integration..*(..))")
    public Object aroundIntegrationCall(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> clazz = signature.getDeclaringType();
        String className = clazz.getSimpleName();
        String methodName = signature.getMethod().getName();

        String serviceName = resolveServiceName(clazz.getPackageName());

        String spanName = "external." + serviceName + "." + methodName;

        Span newSpan = tracer.nextSpan().name(spanName);
        long startNanos = System.nanoTime();
        boolean success = false;
        Throwable error = null;

        try (Tracer.SpanInScope ws = tracer.withSpan(newSpan.start())) {
            newSpan.tag("integration.service", serviceName);
            newSpan.tag("integration.class", className);
            newSpan.tag("integration.method", methodName);

            Object result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            error = ex;
            newSpan.error(ex);
            throw ex;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            newSpan.tag("integration.outcome", success ? "SUCCESS" : "ERROR");
            newSpan.end();

            List<Tag> tags = List.of(
                    Tag.of("service", serviceName),
                    Tag.of("class", className),
                    Tag.of("method", methodName),
                    Tag.of("outcome", success ? "SUCCESS" : "ERROR"));

            Timer timer = Timer.builder("cinelog.integration.duration")
                    .description("Duração de chamadas a integrações externas")
                    .tags(tags)
                    .register(meterRegistry);

            timer.record(durationNanos, TimeUnit.NANOSECONDS);

            if (!success && error != null) {
                log.warn("Erro em integração externa service={} class={} method={} error={}",
                        serviceName, className, methodName, error.toString());
            }
        }
    }

    private String resolveServiceName(String packageName) {
        if (packageName.contains(".tmdb")) {
            return "tmdb";
        }
        // No futuro você pode mapear outros: ".payment", ".recommendation", etc.
        return "unknown";
    }
}
