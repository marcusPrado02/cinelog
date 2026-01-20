package com.cine.cinelog.shared.observability.aop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.Tag;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspecto responsável por instrumentar métodos das camadas de aplicação e
 * features
 * para expor métricas de tempo de execução e erros.
 *
 * Regras:
 * - Aplica-se a:
 * * Métodos públicos em com.cine.cinelog.core.application.usecase..
 * * Métodos públicos em com.cine.cinelog.features..
 * * Qualquer método anotado com @Measured (em classe ou método).
 *
 * - Métricas expostas:
 * * Timer: cinelog.method.execution
 * * Counter: cinelog.method.errors
 *
 * - Tags:
 * * class -> nome simples da classe (ex: SyncMediaFromTmdbService)
 * * method -> nome do método
 * * layer -> application | features | other
 * * outcome -> SUCCESS | ERROR
 * * exception -> tipo da exceção (ou "NONE" em sucesso)
 *
 * Integração com tracing:
 * - Anexa tags ao span atual, se existir.
 */
@Aspect
@Component
@Slf4j
@ConditionalOnProperty(prefix = "cinelog.metrics.aspect", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MetricsAspect {

    private final MeterRegistry meterRegistry;
    private final Tracer tracer; // pode ser null se tracing não estiver configurado

    @Autowired
    public MetricsAspect(MeterRegistry meterRegistry, @Autowired(required = false) Tracer tracer) {
        this.meterRegistry = meterRegistry;
        this.tracer = tracer;
    }

    @Around("execution(public * com.cine.cinelog.core.application.usecase..*(..)) || " +
            "execution(public * com.cine.cinelog.features..*(..)) || " +
            "@annotation(com.cine.cinelog.shared.observability.Measured) || " +
            "@within(com.cine.cinelog.shared.observability.Measured)")
    public Object aroundMonitoredMethods(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Class<?> declaringType = signature.getDeclaringType();
        String className = declaringType.getSimpleName();
        String methodName = signature.getMethod().getName();
        String layer = resolveLayer(declaringType.getPackageName());

        Measured measuredOnMethod = signature.getMethod().getAnnotation(Measured.class);
        Measured measuredOnClass = declaringType.getAnnotation(Measured.class);

        // Se algum @Measured explicitamente disabled, não mede
        if (isMeasurementDisabled(measuredOnMethod, measuredOnClass)) {
            return pjp.proceed();
        }

        String metricBaseName = resolveMetricName(measuredOnMethod, measuredOnClass);

        List<Tag> baseTags = new ArrayList<>();
        baseTags.add(Tag.of("class", className));
        baseTags.add(Tag.of("method", methodName));
        baseTags.add(Tag.of("layer", layer));

        // Enriquecer span atual (se houver) com tags de contexto
        enrichCurrentSpan(className, methodName, layer);

        long startNanos = System.nanoTime();
        boolean success = false;
        Throwable error = null;

        try {
            Object result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;

            String outcome = success ? "SUCCESS" : "ERROR";
            String exceptionType = success ? "NONE" : error.getClass().getSimpleName();

            List<Tag> tags = new ArrayList<>(baseTags);
            tags.add(Tag.of("outcome", outcome));
            tags.add(Tag.of("exception", exceptionType));

            // Timer
            Timer timer = Timer.builder(metricBaseName)
                    .description("Tempo de execução de métodos instrumentados")
                    .tags(tags)
                    .register(meterRegistry);

            timer.record(durationNanos, java.util.concurrent.TimeUnit.NANOSECONDS);

            // Counter de erro
            if (!success) {
                Counter errorCounter = Counter.builder(metricBaseName + ".errors")
                        .description("Total de erros em métodos instrumentados")
                        .tags(baseTags) // sem outcome/exception para evitar muita cardinalidade
                        .register(meterRegistry);
                errorCounter.increment();

                log.debug("Erro em método instrumentado: {}.{} - exception={}",
                        className, methodName, exceptionType);
            }
        }
    }

    private boolean isMeasurementDisabled(Measured methodAnn, Measured classAnn) {
        if (methodAnn != null && !methodAnn.enabled()) {
            return true;
        }
        if (classAnn != null && !classAnn.enabled()) {
            return true;
        }
        return false;
    }

    private String resolveMetricName(Measured methodAnn, Measured classAnn) {
        if (methodAnn != null && !methodAnn.value().isBlank()) {
            return methodAnn.value();
        }
        if (classAnn != null && !classAnn.value().isBlank()) {
            return classAnn.value();
        }
        // default global
        return "cinelog.method.execution";
    }

    private String resolveLayer(String packageName) {
        if (packageName.contains(".core.application.")) {
            return "application";
        }
        if (packageName.contains(".core.domain.")) {
            return "domain";
        }
        if (packageName.contains(".features.")) {
            return "features";
        }
        return "other";
    }

    private void enrichCurrentSpan(String className, String methodName, String layer) {
        if (tracer == null) {
            return;
        }
        var span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        span.tag("cinelog.class", className);
        span.tag("cinelog.method", methodName);
        span.tag("cinelog.layer", layer);
    }
}
