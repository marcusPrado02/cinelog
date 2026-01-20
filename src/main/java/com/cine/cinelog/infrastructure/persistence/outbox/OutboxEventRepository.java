package com.cine.cinelog.infrastructure.persistence.outbox;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository para OutboxEventEntity.
 *
 * @since 1.1.0
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, UUID> {

    /**
     * Busca eventos prontos para processamento (PENDING ou FAILED ready for retry).
     *
     * @param now      Instant atual para comparação de next_retry_at
     * @param pageable Paginação
     * @return Página de eventos prontos
     */
    @Query("""
            SELECT e FROM OutboxEventEntity e
            WHERE e.status = 'PENDING'
               OR (e.status = 'FAILED' AND e.nextRetryAt <= :now)
            ORDER BY e.createdAt ASC
            """)
    Page<OutboxEventEntity> findReadyForProcessing(Instant now, Pageable pageable);

    /**
     * Busca eventos por status.
     */
    List<OutboxEventEntity> findByStatus(OutboxEventEntity.OutboxStatus status);

    /**
     * Busca eventos de um agregado específico.
     */
    List<OutboxEventEntity> findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            String aggregateType,
            String aggregateId);

    /**
     * Busca eventos antigos já processados para limpeza (housekeeping).
     *
     * @param status Status do evento
     * @param before Instant limite (eventos processados antes disso)
     * @return Lista de eventos para cleanup
     */
    List<OutboxEventEntity> findByStatusAndProcessedAtBefore(
            OutboxEventEntity.OutboxStatus status,
            Instant before);

    /**
     * Conta eventos por status.
     */
    long countByStatus(OutboxEventEntity.OutboxStatus status);
}
