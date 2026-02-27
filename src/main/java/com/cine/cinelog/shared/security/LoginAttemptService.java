package com.cine.cinelog.shared.security;

import com.cine.cinelog.shared.observability.security.SecurityEvent;
import com.cine.cinelog.shared.observability.security.SecurityEventLogger;
import com.cine.cinelog.shared.observability.security.SecurityMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A07:2025 — Rastreio de tentativas de login e bloqueio de conta (account
 * lockout).
 *
 * <p>
 * Após {@code maxAttempts} tentativas falhas, o IP/email é bloqueado por
 * {@code lockDurationSeconds}. Usa ConcurrentHashMap para thread-safety.
 * </p>
 *
 * <p>
 * <b>Design decisions:</b>
 * </p>
 * <ul>
 * <li>Bloqueio por EMAIL (previne brute force em conta específica)</li>
 * <li>Bloqueio por IP (previne credential stuffing distribuído)</li>
 * <li>In-memory para simplicidade — migrável para Redis para clusters</li>
 * <li>Cleanup automático de entradas expiradas a cada verificação</li>
 * </ul>
 */
@Service
@Slf4j
public class LoginAttemptService {

    private final int maxAttempts;
    private final long lockDurationSeconds;

    /** A09:2025 — Logger e métricas de segurança. */
    private final SecurityEventLogger securityEventLogger;
    private final SecurityMetricsService securityMetrics;

    private final Map<String, AttemptRecord> attemptsMap = new ConcurrentHashMap<>();

    public LoginAttemptService(
            @Value("${cinelog.security.auth.max-login-attempts:5}") int maxAttempts,
            @Value("${cinelog.security.auth.lock-duration-seconds:900}") long lockDurationSeconds,
            SecurityEventLogger securityEventLogger,
            SecurityMetricsService securityMetrics) {
        this.maxAttempts = maxAttempts;
        this.lockDurationSeconds = lockDurationSeconds;
        this.securityEventLogger = securityEventLogger;
        this.securityMetrics = securityMetrics;
        log.info("LoginAttemptService: maxAttempts={}, lockDuration={}s", maxAttempts, lockDurationSeconds);
    }

    /**
     * Registra uma tentativa de login falha para a chave (email ou IP).
     */
    public void recordFailedAttempt(String key) {
        attemptsMap.compute(key, (k, record) -> {
            if (record == null || record.isExpired(lockDurationSeconds)) {
                return new AttemptRecord(1, Instant.now());
            }
            record.increment();
            return record;
        });

        AttemptRecord rec = attemptsMap.get(key);
        if (rec != null && rec.getCount() >= maxAttempts) {
            log.warn("A07:2025 — Conta bloqueada por excesso de tentativas: key_hash={}",
                    hashKey(key));
            // A09:2025 — Evento de segurança CRITICAL + métrica
            securityEventLogger.log(SecurityEvent.AUTH_LOCKED, Map.of(
                    "key_hash", hashKey(key),
                    "attempts", rec.getCount()));
            securityMetrics.incrementAccountLockout();
        }
    }

    /**
     * Registra um login bem-sucedido, limpando o contador de falhas.
     */
    public void recordSuccessfulLogin(String key) {
        attemptsMap.remove(key);
    }

    /**
     * Verifica se a chave está bloqueada (excedeu maxAttempts e ainda dentro da
     * janela).
     */
    public boolean isBlocked(String key) {
        AttemptRecord record = attemptsMap.get(key);
        if (record == null) {
            return false;
        }
        // Se expirou, limpa e retorna não bloqueado
        if (record.isExpired(lockDurationSeconds)) {
            attemptsMap.remove(key);
            return false;
        }
        return record.getCount() >= maxAttempts;
    }

    /**
     * Retorna quantas tentativas restam antes do bloqueio (0 se já bloqueado).
     */
    public int getRemainingAttempts(String key) {
        AttemptRecord record = attemptsMap.get(key);
        if (record == null || record.isExpired(lockDurationSeconds)) {
            return maxAttempts;
        }
        return Math.max(0, maxAttempts - record.getCount());
    }

    /**
     * Retorna em quantos segundos o bloqueio expira (0 se não está bloqueado).
     */
    public long getSecondsUntilUnlock(String key) {
        AttemptRecord record = attemptsMap.get(key);
        if (record == null || !isBlocked(key)) {
            return 0;
        }
        long elapsed = Instant.now().getEpochSecond() - record.getFirstAttempt().getEpochSecond();
        return Math.max(0, lockDurationSeconds - elapsed);
    }

    /**
     * Hash parcial da chave para logging seguro (não expõe email/IP nos logs).
     */
    private String hashKey(String key) {
        if (key.length() <= 4) {
            return "****";
        }
        return key.substring(0, 2) + "***" + key.substring(key.length() - 2);
    }

    /**
     * Registro interno de tentativas de login.
     */
    private static class AttemptRecord {
        private final AtomicInteger count;
        private final Instant firstAttempt;

        AttemptRecord(int initialCount, Instant firstAttempt) {
            this.count = new AtomicInteger(initialCount);
            this.firstAttempt = firstAttempt;
        }

        int getCount() {
            return count.get();
        }

        void increment() {
            count.incrementAndGet();
        }

        Instant getFirstAttempt() {
            return firstAttempt;
        }

        /**
         * A janela de lock expirou? Se sim, as tentativas são resetadas.
         */
        boolean isExpired(long lockDurationSeconds) {
            return Instant.now().isAfter(firstAttempt.plusSeconds(lockDurationSeconds));
        }
    }
}
