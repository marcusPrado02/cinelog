package com.cine.cinelog.infrastructure.persistence.dlq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DeadLetterEvent persistence.
 * <p>
 * Provides queries for admin operations:
 * - List DLQ events by status, topic, consumer group
 * - Count pending/replayed/ignored events
 * - Find events by eventId (deduplication)
 * <p>
 * Related to:
 * - PR3: DLQ Registry + Admin Endpoints
 * - ADR-006: Kafka Outbox Idempotent Consumer
 *
 * @see DeadLetterEventEntity
 * @see com.cine.cinelog.core.application.services.DeadLetterService
 */
@Repository
public interface DeadLetterEventRepository extends JpaRepository<DeadLetterEventEntity, Long> {

    /**
     * Find all DLQ events by status (paginated).
     * Ordered by created_at DESC (newest first).
     *
     * @param status   DLQ status filter
     * @param pageable Pagination parameters
     * @return Page of matching events
     */
    Page<DeadLetterEventEntity> findByStatusOrderByCreatedAtDesc(
            DeadLetterEventEntity.DlqStatus status,
            Pageable pageable);

    /**
     * Find all DLQ events by status and original topic (paginated).
     *
     * @param status        DLQ status filter
     * @param originalTopic Topic filter
     * @param pageable      Pagination parameters
     * @return Page of matching events
     */
    Page<DeadLetterEventEntity> findByStatusAndOriginalTopicOrderByCreatedAtDesc(
            DeadLetterEventEntity.DlqStatus status,
            String originalTopic,
            Pageable pageable);

    /**
     * Find all DLQ events by consumer group (paginated).
     *
     * @param consumerGroup Consumer group filter
     * @param pageable      Pagination parameters
     * @return Page of matching events
     */
    Page<DeadLetterEventEntity> findByConsumerGroupOrderByCreatedAtDesc(
            String consumerGroup,
            Pageable pageable);

    /**
     * Find DLQ event by eventId (for deduplication check).
     *
     * @param eventId Unique event ID from EventEnvelope
     * @return Optional DeadLetterEvent
     */
    Optional<DeadLetterEventEntity> findByEventId(String eventId);

    /**
     * Count events by status.
     *
     * @param status DLQ status
     * @return Count of matching events
     */
    long countByStatus(DeadLetterEventEntity.DlqStatus status);

    /**
     * Count events by status and topic.
     *
     * @param status        DLQ status
     * @param originalTopic Topic filter
     * @return Count of matching events
     */
    long countByStatusAndOriginalTopic(
            DeadLetterEventEntity.DlqStatus status,
            String originalTopic);

    /**
     * Find events created after a specific timestamp.
     * Useful for monitoring recent failures.
     *
     * @param createdAt Timestamp threshold
     * @param pageable  Pagination parameters
     * @return Page of recent events
     */
    Page<DeadLetterEventEntity> findByCreatedAtAfterOrderByCreatedAtDesc(
            Instant createdAt,
            Pageable pageable);

    /**
     * Find all distinct topics that have DLQ events.
     * Useful for admin UI dropdowns.
     *
     * @return List of unique topic names
     */
    @Query("SELECT DISTINCT d.originalTopic FROM DeadLetterEventEntity d ORDER BY d.originalTopic")
    List<String> findDistinctTopics();

    /**
     * Find all distinct consumer groups that have DLQ events.
     * Useful for admin UI dropdowns.
     *
     * @return List of unique consumer group names
     */
    @Query("SELECT DISTINCT d.consumerGroup FROM DeadLetterEventEntity d ORDER BY d.consumerGroup")
    List<String> findDistinctConsumerGroups();

    /**
     * Count events by status within a time range.
     * Useful for dashboards/metrics.
     *
     * @param status    DLQ status
     * @param startTime Start of time range
     * @param endTime   End of time range
     * @return Count of matching events
     */
    @Query("SELECT COUNT(d) FROM DeadLetterEventEntity d " +
            "WHERE d.status = :status " +
            "AND d.createdAt BETWEEN :startTime AND :endTime")
    long countByStatusAndCreatedAtBetween(
            @Param("status") DeadLetterEventEntity.DlqStatus status,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Check if eventId already exists (deduplication).
     *
     * @param eventId Event ID to check
     * @return true if exists
     */
    boolean existsByEventId(String eventId);
}
