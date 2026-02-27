package com.cine.cinelog.shared.observability.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A08:2025 — Entidade de audit trail com hash chain.
 *
 * <p>
 * Cada registro contém o hash do registro anterior, formando uma cadeia
 * imutável (blockchain-lite). Se qualquer registro for adulterado no banco,
 * a cadeia se quebra e a verificação {@link AuditIntegrityService#verifyChain}
 * detecta a adulteração.
 * </p>
 *
 * @since 1.3.0
 */
@Entity
@Table(name = "audit_integrity_log", indexes = {
        @Index(name = "idx_audit_integrity_entity_type", columnList = "entity_type"),
        @Index(name = "idx_audit_integrity_entity_id", columnList = "entity_id"),
        @Index(name = "idx_audit_integrity_created", columnList = "created_at"),
        @Index(name = "idx_audit_integrity_sequence", columnList = "sequence_number")
})
@Getter
@Setter
@NoArgsConstructor
public class AuditIntegrityLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Número sequencial global para ordenação da cadeia.
     */
    @Column(name = "sequence_number", nullable = false, unique = true)
    private Long sequenceNumber;

    /**
     * Tipo da entidade auditada (ex: "UserEntity", "WatchEntryEntity").
     */
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    /**
     * ID da entidade auditada.
     */
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    /**
     * Ação realizada: CREATE, UPDATE, DELETE.
     */
    @Column(name = "action", nullable = false, length = 20)
    private String action;

    /**
     * HMAC-SHA256 dos campos críticos da entidade no momento da operação.
     */
    @Column(name = "entity_hmac", nullable = false, length = 64)
    private String entityHmac;

    /**
     * Hash do registro anterior na cadeia.
     * Primeiro registro: "GENESIS".
     */
    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    /**
     * Hash deste registro =
     * HMAC(sequenceNumber|entityType|entityId|action|entityHmac|previousHash).
     * Para verificar a cadeia, recalcule este hash e compare.
     */
    @Column(name = "record_hash", nullable = false, length = 64)
    private String recordHash;

    /**
     * ID do usuário que executou a ação.
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Timestamp da operação.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Descrição legível da mudança (opcional).
     */
    @Column(name = "description", length = 500)
    private String description;
}
