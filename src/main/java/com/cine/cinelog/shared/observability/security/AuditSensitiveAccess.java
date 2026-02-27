package com.cine.cinelog.shared.observability.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A09:2025 — Marca métodos cuja execução acessa dados sensíveis e deve
 * ser registrada no log de segurança.
 *
 * <h3>Por que auditar acesso a dados sensíveis?</h3>
 * <p>
 * Regulamentações como <b>LGPD</b> (Art. 37), <b>GDPR</b> (Art. 30) e
 * <b>PCI-DSS</b> (Req. 10) exigem que toda operação envolvendo dados
 * pessoais seja rastreável: quem acessou, quando, qual recurso.
 * </p>
 *
 * <h3>Exemplo de uso:</h3>
 * 
 * <pre>
 * {@code
 * &#64;AuditSensitiveAccess(resource = "user_profile", action = "VIEW")
 * public UserDTO getUserById(Long id) { ... }
 *
 * @AuditSensitiveAccess(resource = "payment_info", action = "EXPORT")
 * public byte[] exportPaymentData() { ... }
 * }
 * </pre>
 *
 * @since 1.2
 * @see DataAccessAuditAspect
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditSensitiveAccess {

    /**
     * Tipo do recurso sensível acessado (ex.: "user_profile", "payment_info").
     */
    String resource();

    /**
     * Ação realizada (ex.: "VIEW", "UPDATE", "EXPORT", "DELETE").
     */
    String action() default "VIEW";
}
