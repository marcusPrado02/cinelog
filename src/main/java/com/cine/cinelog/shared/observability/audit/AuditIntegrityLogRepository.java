package com.cine.cinelog.shared.observability.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * A08:2025 — Repository para audit trail com hash chain.
 *
 * @since 1.3.0
 */
@Repository
public interface AuditIntegrityLogRepository extends JpaRepository<AuditIntegrityLogEntity, Long> {

    /**
     * Retorna o último registro na cadeia (maior sequence_number).
     */
    Optional<AuditIntegrityLogEntity> findTopByOrderBySequenceNumberDesc();

    /**
     * Retorna todos os registros ordenados por sequência (para verificação da
     * cadeia).
     */
    List<AuditIntegrityLogEntity> findAllByOrderBySequenceNumberAsc();

    /**
     * Retorna registros por tipo e ID de entidade para auditoria específica.
     */
    List<AuditIntegrityLogEntity> findByEntityTypeAndEntityIdOrderBySequenceNumberAsc(
            String entityType, String entityId);

    /**
     * Retorna o próximo valor de sequência.
     */
    @Query("SELECT COALESCE(MAX(a.sequenceNumber), 0) + 1 FROM AuditIntegrityLogEntity a")
    Long nextSequenceNumber();
}
