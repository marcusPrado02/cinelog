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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Aspect de "security boundary" que registra telemetria de acesso a operações
 * sensíveis
 * e, opcionalmente, faz enforcement de permissão com base em authorities.
 *
 * Métrica:
 * - cinelog.security.access_total
 * Tags:
 * - module, operation, outcome (ALLOWED/DENIED), username
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "cinelog.security", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityBoundaryAspect {

    private final MeterRegistry meterRegistry;

    @Around("@annotation(secureOperation)")
    public Object aroundSecureOperation(ProceedingJoinPoint pjp, SecureOperation secureOperation) throws Throwable {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();

        String module = secureOperation.module().isBlank() ? "UNKNOWN" : secureOperation.module();
        String requiredPermission = secureOperation.value();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null && auth.getName() != null ? auth.getName() : "anonymous";

        boolean authorized = isAuthorized(auth, requiredPermission);
        String outcome = authorized ? "ALLOWED" : "DENIED";

        List<Tag> tags = List.of(
                Tag.of("module", module),
                Tag.of("operation", requiredPermission.isBlank() ? methodName : requiredPermission),
                Tag.of("outcome", outcome),
                Tag.of("username", username));

        Counter counter = Counter.builder("cinelog.security.access_total")
                .description("Tentativas de acesso a operações sensíveis")
                .tags(tags)
                .register(meterRegistry);

        counter.increment();

        if (!authorized && secureOperation.enforce()) {
            log.warn("Acesso negado em {}.{} para usuário={} permissão={}",
                    className, methodName, username, requiredPermission);
            throw new AccessDeniedException("Access denied to operation: " + requiredPermission);
        }

        if (!authorized) {
            log.warn("Tentativa de acesso não autorizado em {}.{} para usuário={} permissão={}",
                    className, methodName, username, requiredPermission);
        } else {
            log.debug("Acesso permitido em {}.{} para usuário={} permissão={}",
                    className, methodName, username, requiredPermission);
        }

        return pjp.proceed();
    }

    private boolean isAuthorized(Authentication auth, String requiredPermission) {
        if (requiredPermission == null || requiredPermission.isBlank()) {
            // Sem permissão específica configurada, não fazemos enforcement.
            return true;
        }
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        Collection<?> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }

        return authorities.stream()
                .map(Object::toString)
                .anyMatch(requiredPermission::equals);
    }
}
