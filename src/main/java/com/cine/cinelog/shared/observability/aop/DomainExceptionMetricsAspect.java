package com.cine.cinelog.shared.observability.aop;

import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Aspect responsável por registrar métricas sempre que um DomainException é
 * lançado
 * em métodos da camada de aplicação (usecases).
 *
 * Métrica exposta:
 * - cinelog.domain.error_total
 * Tags:
 * - usecase, method, errorCode
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cinelog.metrics.domain", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DomainExceptionMetricsAspect {

    private final MeterRegistry meterRegistry;

    @AfterThrowing(pointcut = "execution(public * com.cine.cinelog.core.application.usecase..*(..))", throwing = "ex")
    public void onDomainException(JoinPoint joinPoint, DomainException ex) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String useCaseName = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        ErrorCode errorCode = ex.getErrorCode();
        String errorCodeName = errorCode != null ? errorCode.name() : "UNKNOWN";

        List<Tag> tags = List.of(
                Tag.of("usecase", useCaseName),
                Tag.of("method", methodName),
                Tag.of("errorCode", errorCodeName));

        Counter counter = Counter.builder("cinelog.domain.error_total")
                .description("Total de erros de domínio (DomainException) por usecase/método/código de erro")
                .tags(tags)
                .register(meterRegistry);

        counter.increment();

        log.warn("DomainException capturada em {}.{} - errorCode={}, message={}",
                useCaseName, methodName, errorCodeName, ex.getMessage());
    }
}
