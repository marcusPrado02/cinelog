package com.cine.cinelog.shared.observability.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Aspect responsável por registrar trilha de auditoria para métodos anotados
 * com {@link AuditableAction}.
 *
 * Implementação simples: escreve em um logger dedicado (AUDIT_LOGGER) em
 * formato estruturado.
 * Futuro: pode chamar uma porta de saída para persistir em banco ou enviar para
 * um tópico.
 */
@Aspect
@Component
@Slf4j(topic = "AUDIT_LOGGER")
@ConditionalOnProperty(prefix = "cinelog.audit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuditTrailAspect {

    @Around("@annotation(auditableAction)")
    public Object aroundAuditableMethod(ProceedingJoinPoint pjp, AuditableAction auditableAction) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        Instant startTime = Instant.now();
        boolean success = false;
        Object result = null;
        Throwable error = null;

        try {
            result = pjp.proceed();
            success = true;
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            Instant endTime = Instant.now();

            Map<String, Object> audit = new HashMap<>();
            audit.put("timestamp", startTime.toString());
            audit.put("endTimestamp", endTime.toString());
            audit.put("module", auditableAction.module());
            audit.put("action", auditableAction.action());
            audit.put("description", auditableAction.description());
            audit.put("class", className);
            audit.put("method", methodName);
            audit.put("success", success);

            // Placeholder: integrar com seu contexto de segurança no futuro
            audit.put("userId", null);

            // Exemplo: se retornar Media, extrair info relevante
            if (result instanceof com.cine.cinelog.core.domain.model.Media media) {
                audit.put("mediaId", media.getId());
                audit.put("mediaTitle", media.getTitle());
            }

            if (!success && error != null) {
                audit.put("errorType", error.getClass().getSimpleName());
                audit.put("errorMessage", error.getMessage());
            }

            // Saída em formato estruturado (será serializado em JSON pelo logstash encoder)
            log.info("audit={}", audit);
        }
    }
}
