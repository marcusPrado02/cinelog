package com.cine.cinelog.shared.observability.security;

/**
 * A09:2025 — Taxonomia de eventos de segurança.
 *
 * <p>
 * Cada evento possui:
 * </p>
 * <ul>
 * <li><b>code</b> — identificador curto e estável (ex.: {@code SEC-001}),
 * usado como chave em alertas e dashboards.</li>
 * <li><b>severity</b> — nível de gravidade que determina o appender/destino
 * e a urgência de alerta.</li>
 * <li><b>description</b> — texto legível para humanos (aparece nos logs
 * JSON).</li>
 * <li><b>alertable</b> — se {@code true}, o evento é encaminhado ao
 * {@link SecurityAlertService} para avaliação de threshold e notificação.</li>
 * </ul>
 *
 * <h3>Níveis de severidade</h3>
 * <table>
 * <tr>
 * <th>Severidade</th>
 * <th>SLF4J</th>
 * <th>Exemplo</th>
 * </tr>
 * <tr>
 * <td>INFO</td>
 * <td>info</td>
 * <td>Login bem-sucedido, token refreshed</td>
 * </tr>
 * <tr>
 * <td>WARNING</td>
 * <td>warn</td>
 * <td>Tentativa de login falha, rate limit</td>
 * </tr>
 * <tr>
 * <td>CRITICAL</td>
 * <td>error</td>
 * <td>Account lockout, JWT adulterado</td>
 * </tr>
 * <tr>
 * <td>ALERT</td>
 * <td>error</td>
 * <td>SQL injection, tamper detection, brute force</td>
 * </tr>
 * </table>
 *
 * @since 1.2
 */
public enum SecurityEvent {

    // ─── Autenticação ────────────────────────────────────────────
    AUTH_SUCCESS("SEC-001", Severity.INFO, "Login bem-sucedido", false),
    AUTH_FAILURE("SEC-002", Severity.WARNING, "Tentativa de login falha", true),
    AUTH_LOCKED("SEC-003", Severity.CRITICAL, "Conta bloqueada por excesso de falhas", true),
    LOGOUT("SEC-004", Severity.INFO, "Logout realizado", false),

    // ─── Tokens ──────────────────────────────────────────────────
    JWT_INVALID("SEC-010", Severity.WARNING, "JWT inválido recebido", true),
    JWT_EXPIRED("SEC-011", Severity.INFO, "JWT expirado", false),
    TOKEN_REFRESHED("SEC-012", Severity.INFO, "Token refreshed com sucesso", false),
    TOKEN_REVOKED("SEC-013", Severity.INFO, "Refresh token revogado", false),
    TOKEN_REUSE_DETECTED("SEC-014", Severity.CRITICAL, "Reuso de refresh token detectado", true),

    // ─── Autorização ─────────────────────────────────────────────
    ACCESS_DENIED("SEC-020", Severity.WARNING, "Acesso negado a recurso protegido", true),
    PRIVILEGE_ESCALATION("SEC-021", Severity.ALERT, "Tentativa de escalação de privilégios", true),

    // ─── Rate Limiting ───────────────────────────────────────────
    RATE_LIMITED("SEC-030", Severity.WARNING, "Requisição bloqueada por rate limit", true),

    // ─── Injection / Tamper ──────────────────────────────────────
    SQL_INJECTION_ATTEMPT("SEC-040", Severity.ALERT, "Padrão de SQL Injection detectado", true),
    TAMPER_DETECTED("SEC-041", Severity.ALERT, "Adulteração de dados detectada", true),
    INTEGRITY_VIOLATION("SEC-042", Severity.ALERT, "Violação de integridade (hash chain)", true),

    // ─── Dados sensíveis ─────────────────────────────────────────
    SENSITIVE_DATA_ACCESS("SEC-050", Severity.INFO, "Acesso a dados sensíveis registrado", false),
    PASSWORD_CHANGED("SEC-051", Severity.INFO, "Senha alterada", false),

    // ─── Configuração / Sistema ──────────────────────────────────
    SECURITY_CONFIG_CHANGE("SEC-060", Severity.WARNING, "Alteração em configuração de segurança", true),
    SUSPICIOUS_ACTIVITY("SEC-070", Severity.CRITICAL, "Atividade suspeita detectada", true);

    private final String code;
    private final Severity severity;
    private final String description;
    private final boolean alertable;

    SecurityEvent(String code, Severity severity, String description, boolean alertable) {
        this.code = code;
        this.severity = severity;
        this.description = description;
        this.alertable = alertable;
    }

    public String getCode() {
        return code;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String getDescription() {
        return description;
    }

    public boolean isAlertable() {
        return alertable;
    }

    /**
     * Níveis de severidade para eventos de segurança.
     * Mapeados diretamente para níveis SLF4J na saída de log.
     */
    public enum Severity {
        /** Informativo — operação normal de segurança. */
        INFO,
        /** Atenção — possível tentativa de ataque ou configuração inadequada. */
        WARNING,
        /** Crítico — incidente confirmado que requer investigação. */
        CRITICAL,
        /** Alerta — ataque em andamento que requer resposta imediata. */
        ALERT
    }
}
