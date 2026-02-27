package com.cine.cinelog.shared.observability.security;

import com.cine.cinelog.shared.security.InputSanitizer;
import com.cine.cinelog.shared.security.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A09:2025 — Logger centralizado de eventos de segurança.
 *
 * <h3>Por que um logger dedicado?</h3>
 * <p>
 * Logs de segurança precisam de tratamento diferenciado:
 * </p>
 * <ul>
 * <li><b>Segregação:</b> arquivo separado com retenção mais longa (90 dias vs
 * 30)</li>
 * <li><b>Imutabilidade:</b> encaminhados para SIEM/Logstash em tempo real</li>
 * <li><b>Estrutura:</b> campos padronizados para correlação (eventCode,
 * severity, IP, userId)</li>
 * <li><b>Mascaramento:</b> dados sensíveis NUNCA aparecem em logs de
 * segurança</li>
 * </ul>
 *
 * <h3>Marker-based routing</h3>
 * <p>
 * Usa SLF4J {@link Marker} com nome {@code SECURITY} para que o logback
 * encaminhe esses eventos para o appender dedicado {@code SECURITY_FILE}
 * sem duplicar na saída geral.
 * </p>
 *
 * <h3>Campos estruturados</h3>
 * <p>
 * Cada evento gera um mapa JSON com campos fixos:
 * </p>
 * 
 * <pre>
 * {
 *   "securityEvent": "SEC-002",
 *   "severity": "WARNING",
 *   "description": "Tentativa de login falha",
 *   "principal": "us***@example.com",
 *   "clientIp": "192.168.1.100",
 *   "traceId": "abc123",
 *   "timestamp": "2025-01-15T10:30:00Z",
 *   "details": { ... }
 * }
 * </pre>
 *
 * @since 1.2
 * @see SecurityEvent
 * @see SecurityAlertService
 */
@Component
public class SecurityEventLogger {

    /**
     * Logger dedicado a eventos de segurança.
     * Topic "SECURITY" permite configuração independente no logback.
     */
    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");

    /**
     * Marker SLF4J para routing no logback.
     * O appender SECURITY_FILE filtra por este marker.
     */
    public static final Marker SECURITY_MARKER = MarkerFactory.getMarker("SECURITY");

    private final SensitiveDataMasker masker;
    private final SecurityAlertService alertService;

    public SecurityEventLogger(SensitiveDataMasker masker, SecurityAlertService alertService) {
        this.masker = masker;
        this.alertService = alertService;
    }

    /**
     * Registra um evento de segurança com detalhes contextuais.
     *
     * @param event   tipo do evento (enum {@link SecurityEvent})
     * @param details pares chave-valor com contexto adicional (IP, URI, motivo,
     *                etc.)
     *                Valores sensíveis são mascarados automaticamente.
     */
    public void log(SecurityEvent event, Map<String, Object> details) {
        Map<String, Object> structured = buildStructuredPayload(event, details);

        switch (event.getSeverity()) {
            case INFO -> SECURITY_LOG.info(SECURITY_MARKER,
                    "security_event={}", structured);
            case WARNING -> SECURITY_LOG.warn(SECURITY_MARKER,
                    "security_event={}", structured);
            case CRITICAL -> SECURITY_LOG.error(SECURITY_MARKER,
                    "security_event={}", structured);
            case ALERT -> SECURITY_LOG.error(SECURITY_MARKER,
                    "ALERT security_event={}", structured);
        }

        // Encaminha para avaliação de threshold se o evento for alertable
        if (event.isAlertable()) {
            alertService.evaluate(event, structured);
        }
    }

    /**
     * Versão simplificada sem detalhes adicionais.
     */
    public void log(SecurityEvent event) {
        log(event, Map.of());
    }

    /**
     * Versão com mensagem simples como detalhe.
     */
    public void log(SecurityEvent event, String message) {
        log(event, Map.of("message", message));
    }

    /**
     * Monta o payload estruturado completo com campos fixos + MDC + detalhes.
     */
    private Map<String, Object> buildStructuredPayload(SecurityEvent event,
            Map<String, Object> details) {
        Map<String, Object> payload = new LinkedHashMap<>();

        // Campos fixos do evento
        payload.put("securityEvent", event.getCode());
        payload.put("eventName", event.name());
        payload.put("severity", event.getSeverity().name());
        payload.put("description", event.getDescription());
        payload.put("timestamp", Instant.now().toString());

        // Contexto do MDC (já populado pelo ObservabilityContextFilter)
        payload.put("traceId", MDC.get("traceId"));
        payload.put("clientIp", MDC.get("clientIp"));
        payload.put("userAgent", sanitize(MDC.get("userAgent")));

        // Principal autenticado (ou "anonymous")
        payload.put("principal", resolvePrincipal());

        // Detalhes adicionais (mascarados)
        if (details != null && !details.isEmpty()) {
            Map<String, Object> safeDetails = new LinkedHashMap<>();
            for (var entry : details.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (masker.isSensitiveKey(key)) {
                    safeDetails.put(key, "***MASKED***");
                } else if (value instanceof String s) {
                    safeDetails.put(key, sanitize(s));
                } else {
                    safeDetails.put(key, value);
                }
            }
            payload.put("details", safeDetails);
        }

        return payload;
    }

    /**
     * Resolve o principal autenticado, mascarando o email.
     */
    private String resolvePrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getName())) {
            return masker.maskEmail(auth.getName());
        }
        return "anonymous";
    }

    /**
     * Sanitiza strings para prevenir log injection (A05).
     */
    private String sanitize(String value) {
        return value != null ? InputSanitizer.sanitizeForLog(value) : null;
    }
}
