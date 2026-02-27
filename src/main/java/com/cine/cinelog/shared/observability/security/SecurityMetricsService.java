package com.cine.cinelog.shared.observability.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * A09:2025 — Métricas Micrometer dedicadas a segurança.
 *
 * <h3>Por que métricas separadas de BusinessMetricsService?</h3>
 * <p>
 * As métricas de segurança têm responsabilidades distintas:
 * </p>
 * <ul>
 * <li><b>Prefixo diferente:</b> {@code cinelog.security.*} vs
 * {@code cinelog.business.*}</li>
 * <li><b>Consumidores diferentes:</b> equipe de segurança/SOC vs equipe de
 * produto</li>
 * <li><b>Dashboard dedicado:</b> Grafana "Security Overview" com painéis
 * específicos</li>
 * <li><b>Alertas com SLA:</b> SQL injection → PagerDuty em 5 min; media.created
 * → sem alerta</li>
 * </ul>
 *
 * <h3>Métricas expostas</h3>
 * <table>
 * <tr>
 * <th>Métrica</th>
 * <th>Labels</th>
 * <th>Descrição</th>
 * </tr>
 * <tr>
 * <td>cinelog.security.auth_failures_total</td>
 * <td>reason</td>
 * <td>Falhas de autenticação</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.account_lockouts_total</td>
 * <td>—</td>
 * <td>Contas bloqueadas</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.jwt_failures_total</td>
 * <td>reason</td>
 * <td>JWT inválido/expirado</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.rate_limit_total</td>
 * <td>path_class</td>
 * <td>Rate limit atingido</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.sqli_attempts_total</td>
 * <td>—</td>
 * <td>SQL injection bloqueado</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.access_denied_total</td>
 * <td>—</td>
 * <td>Acesso negado (403)</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.tamper_detected_total</td>
 * <td>type</td>
 * <td>Tampering detectado</td>
 * </tr>
 * <tr>
 * <td>cinelog.security.sensitive_access_total</td>
 * <td>resource</td>
 * <td>Acesso a dados sensíveis</td>
 * </tr>
 * </table>
 *
 * @since 1.2
 * @see SecurityEventLogger
 */
@Service
public class SecurityMetricsService {

    private final MeterRegistry meterRegistry;

    public SecurityMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // ── Autenticação ──────────────────────────────────────────────

    /**
     * Incrementa falha de autenticação.
     *
     * @param reason causa: "invalid_credentials", "account_locked",
     *               "password_policy"
     */
    public void incrementAuthFailure(String reason) {
        counter("cinelog.security.auth_failures_total",
                "Falhas de autenticação por motivo",
                Tag.of("reason", reason))
                .increment();
    }

    /**
     * Incrementa bloqueio de conta por excesso de tentativas.
     */
    public void incrementAccountLockout() {
        counter("cinelog.security.account_lockouts_total",
                "Contas bloqueadas por brute force")
                .increment();
    }

    // ── JWT ───────────────────────────────────────────────────────

    /**
     * Incrementa falha de JWT.
     *
     * @param reason causa: "invalid", "expired", "malformed"
     */
    public void incrementJwtFailure(String reason) {
        counter("cinelog.security.jwt_failures_total",
                "Falhas de validação JWT",
                Tag.of("reason", reason))
                .increment();
    }

    // ── Rate Limiting ─────────────────────────────────────────────

    /**
     * Incrementa rate limit atingido.
     *
     * @param pathClass classificação do path: "auth" ou "general"
     */
    public void incrementRateLimit(String pathClass) {
        counter("cinelog.security.rate_limit_total",
                "Requisições bloqueadas por rate limit",
                Tag.of("path_class", pathClass))
                .increment();
    }

    // ── Injection ─────────────────────────────────────────────────

    /**
     * Incrementa tentativa de SQL injection detectada.
     */
    public void incrementSqlInjectionAttempt() {
        counter("cinelog.security.sqli_attempts_total",
                "Tentativas de SQL injection bloqueadas")
                .increment();
    }

    // ── Autorização ───────────────────────────────────────────────

    /**
     * Incrementa acesso negado (403).
     */
    public void incrementAccessDenied() {
        counter("cinelog.security.access_denied_total",
                "Tentativas de acesso negadas (403)")
                .increment();
    }

    // ── Integridade ───────────────────────────────────────────────

    /**
     * Incrementa detecção de tampering.
     *
     * @param type tipo: "request", "kafka", "hash_chain"
     */
    public void incrementTamperDetected(String type) {
        counter("cinelog.security.tamper_detected_total",
                "Detecções de adulteração de dados",
                Tag.of("type", type))
                .increment();
    }

    // ── Dados sensíveis ───────────────────────────────────────────

    /**
     * Incrementa acesso a dados sensíveis (audit trail).
     *
     * @param resource tipo do recurso acessado: "user_profile", "payment_info",
     *                 etc.
     */
    public void incrementSensitiveDataAccess(String resource) {
        counter("cinelog.security.sensitive_access_total",
                "Acessos a dados sensíveis registrados",
                Tag.of("resource", resource))
                .increment();
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Counter counter(String name, String description, Tag... tags) {
        return Counter.builder(name)
                .description(description)
                .tags(List.of(tags))
                .register(meterRegistry);
    }
}
