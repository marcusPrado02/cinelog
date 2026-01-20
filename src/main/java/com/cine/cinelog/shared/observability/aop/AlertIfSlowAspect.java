package com.cine.cinelog.shared.observability.aop;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aspect que monitora métodos anotados com {@link AlertIfSlow} e emite alertas
 * (log + métrica) quando o tempo de execução excede o limite configurado.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cinelog.metrics.slow", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AlertIfSlowAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(alertIfSlow)")
    public Object around(ProceedingJoinPoint pjp, AlertIfSlow alertIfSlow) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return pjp.proceed();
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            long threshold = alertIfSlow.thresholdMs();

            if (elapsed > threshold) {
                MethodSignature signature = (MethodSignature) pjp.getSignature();
                String className = signature.getDeclaringType().getSimpleName();
                String methodName = signature.getMethod().getName();

                log.warn("Método lento detectado: {}.{} demorou {} ms (limite {} ms)",
                        className, methodName, elapsed, threshold);

                List<Tag> tags = List.of(
                        Tag.of("class", className),
                        Tag.of("method", methodName),
                        Tag.of("thresholdMs", String.valueOf(threshold)));

                Counter counter = Counter.builder(alertIfSlow.metricName())
                        .description("Métodos que ultrapassaram o limite de tempo configurado")
                        .tags(tags)
                        .register(meterRegistry);

                counter.increment();
            }
        }
    }
}
