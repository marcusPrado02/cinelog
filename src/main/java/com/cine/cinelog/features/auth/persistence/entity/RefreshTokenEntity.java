package com.cine.cinelog.features.auth.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A07:2025 — Entidade de Refresh Token.
 *
 * <p>
 * Implementa refresh token rotation com detecção de reuso por família.
 * Cada refresh token pertence a uma "família" (tokenFamily). Quando um token
 * é usado para renovar, ele é marcado como revogado e um novo token é emitido
 * na mesma família. Se um token já revogado for reutilizado, toda a família é
 * invalidada
 * (indica comprometimento — token replay attack).
 * </p>
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token", columnList = "token"),
        @Index(name = "idx_refresh_token_family", columnList = "token_family"),
        @Index(name = "idx_refresh_token_user", columnList = "user_id"),
        @Index(name = "idx_refresh_token_expires", columnList = "expires_at")
})
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Token opaco (UUID) — nunca contém informações do usuário. */
    @Column(nullable = false, unique = true, length = 36)
    private String token;

    /**
     * Família do token — todos os tokens gerados a partir do mesmo login
     * compartilham a mesma família. Permite invalidar toda a cadeia em caso de
     * reuso.
     */
    @Column(name = "token_family", nullable = false, length = 36)
    private String tokenFamily;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_email", nullable = false, length = 255)
    private String userEmail;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by", length = 36)
    private String replacedBy;

    /** IP do cliente que criou o token (para auditoria). */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /** User-Agent do cliente (para detecção de anomalias). */
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    public RefreshTokenEntity() {
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isUsable() {
        return !revoked && !isExpired();
    }

    // ── Getters & Setters ──

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenFamily() {
        return tokenFamily;
    }

    public void setTokenFamily(String tokenFamily) {
        this.tokenFamily = tokenFamily;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(String replacedBy) {
        this.replacedBy = replacedBy;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
