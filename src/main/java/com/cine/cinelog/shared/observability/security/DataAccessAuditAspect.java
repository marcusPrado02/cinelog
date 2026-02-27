package com.cine.cinelog.shared.observability.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A09:2025 — Aspect AOP que registra automaticamente acessos a dados sensíveis.
 *
 * <h3>Como funciona?</h3>
 * <ol>
 * <li>Intercepta métodos anotados com {@link AuditSensitiveAccess}.</li>
 * <li>Extrai contexto: quem (principal), o quê (resource), ação (action).</li>
 * <li>Registra via {@link SecurityEventLogger} com evento
 * {@link SecurityEvent#SENSITIVE_DATA_ACCESS}.</li>
 * <li>Incrementa métrica {@code cinelog.security.sensitive_access_total}.</li>
 * </ol>
 *
 * <h3>Por que AOP?</h3>
 * <p>
 * Auditar acesso a dados sensíveis manualmente (em cada método) viola o
 * princípio DRY e é esquecido com frequência. Com AOP + annotation:
 * </p>
 * <ul>
 * <li>Basta anotar o método — zero código de auditoria no service</li>
 * <li>A auditoria é obrigatória (não pode ser "esquecida")</li>
 * <li>Desacoplado: o service não conhece o logging de segurança</li>
 * </ul>
 *
 * @since 1.2
 * @see AuditSensitiveAccess
 * @see SecurityEventLogger
 */
@Aspect
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cinelog.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataAccessAuditAspect {

    private final SecurityEventLogger securityEventLogger;
    private final SecurityMetricsService securityMetrics;

    @Around("@annotation(auditAccess)")
    public Object aroundSensitiveAccess(ProceedingJoinPoint pjp,
            AuditSensitiveAccess auditAccess) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        String resource = auditAccess.resource();
        String action = auditAccess.action();
        String principal = resolvePrincipal();

        Object result;
        boolean success = true;

        try {
            result = pjp.proceed();
        } catch (Throwable ex) {
            success = false;
            // Log de acesso mesmo em caso de erro (tentativa conta)
            logAccess(resource, action, principal, className, methodName, false, ex.getClass().getSimpleName());
            throw ex;
        }

        logAccess(resource, action, principal, className, methodName, success, null);
        return result;
    }

    private void logAccess(String resource, String action, String principal,
            String className, String methodName,
            boolean success, String errorType) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("resource", resource);
        details.put("action", action);
        details.put("class", className);
        details.put("method", methodName);
        details.put("success", success);
        if (errorType != null) {
            details.put("errorType", errorType);
        }

        securityEventLogger.log(SecurityEvent.SENSITIVE_DATA_ACCESS, details);
        securityMetrics.incrementSensitiveDataAccess(resource);
    }

    private String resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName())) {
            return auth.getName();
        }
        return "anonymous";
    }
}
