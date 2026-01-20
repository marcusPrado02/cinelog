package com.cine.cinelog.infrastructure.persistence.dlq;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * JPA Entity representing a failed Kafka event that was sent to the Dead Letter
 * Queue.
 * <p>
 * This entity stores poison messages for operational investigation and replay.
 * Operators can query, inspect, and selectively replay failed events via admin
 * endpoints.
 * <p>
 * Related to:
 * - PR3: DLQ Registry + Admin Endpoints
 * - ADR-006: Kafka Outbox Idempotent Consumer
 *
 * @see com.cine.cinelog.infrastructure.web.controllers.admin.DeadLetterAdminController
 * @see com.cine.cinelog.core.application.services.DeadLetterService
 */
@Entity
@Table(name = "dead_letter_event", indexes = {
        @Index(name = "idx_dlq_status_topic", columnList = "status, original_topic, created_at"),
        @Index(name = "idx_dlq_event_id", columnList = "event_id"),
        @Index(name = "idx_dlq_consumer_group", columnList = "consumer_group, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "envelopeJson", "stackTrace" }) // Avoid printing large text fields
public class DeadLetterEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========== Kafka Context ==========

    /**
     * Original Kafka topic from which the event failed (e.g.,
     * "watch-entry-events").
     */
    @Column(name = "original_topic", nullable = false, length = 255)
    private String originalTopic;

    /**
     * Consumer group ID that encountered the failure (e.g.,
     * "cinelog-watch-consumer").
     */
    @Column(name = "consumer_group", nullable = false, length = 255)
    private String consumerGroup;

    /**
     * Kafka partition number (nullable for backward compatibility).
     */
    @Column(name = "partition_number")
    private Integer partitionNumber;

    /**
     * Kafka offset number (nullable for backward compatibility).
     */
    @Column(name = "offset_number")
    private Long offsetNumber;

    // ========== Event Identification ==========

    /**
     * Unique event ID from the EventEnvelope (UUID format).
     * Used for deduplication and correlation.
     */
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    /**
     * Event type (e.g., "watch_entry_created").
     * Extracted from EventEnvelope for quick filtering.
     */
    @Column(name = "event_type", length = 100)
    private String eventType;

    // ========== Event Content ==========

    /**
     * Full EventEnvelope serialized as JSON.
     * Stored for investigation and replay purposes.
     */
    @Lob
    @Column(name = "envelope_json", nullable = false, columnDefinition = "TEXT")
    private String envelopeJson;

    // ========== Error Details ==========

    /**
     * Error message from the exception that caused the failure.
     */
    @Lob
    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Fully qualified class name of the exception (e.g.,
     * "com.fasterxml.jackson.databind.JsonMappingException").
     */
    @Column(name = "error_class", length = 500)
    private String errorClass;

    /**
     * Full stack trace for debugging (optional).
     * Can be truncated if too large (>64KB).
     */
    @Lob
    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    // ========== Status & Replay Tracking ==========

    /**
     * Current status of the DLQ event.
     * - PENDING_REPLAY: Waiting for operator action
     * - REPLAYED: Successfully replayed and reprocessed
     * - IGNORED: Marked as non-actionable by operator
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DlqStatus status = DlqStatus.PENDING_REPLAY;

    /**
     * Timestamp when the event was successfully replayed (nullable).
     */
    @Column(name = "replayed_at")
    private Instant replayedAt;

    /**
     * Username or system identifier that performed the replay (nullable).
     */
    @Column(name = "replayed_by", length = 100)
    private String replayedBy;

    // ========== Timestamps ==========

    /**
     * When the event was persisted to DLQ (auto-set by DB).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Last update timestamp (auto-updated by DB).
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // ========== Business Methods ==========

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (status == null) {
            status = DlqStatus.PENDING_REPLAY;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    /**
     * Mark this event as successfully replayed.
     *
     * @param replayedBy Username or system identifier
     */
    public void markAsReplayed(String replayedBy) {
        this.status = DlqStatus.REPLAYED;
        this.replayedAt = Instant.now();
        this.replayedBy = replayedBy;
    }

    /**
     * Mark this event as ignored (not actionable).
     */
    public void markAsIgnored() {
        this.status = DlqStatus.IGNORED;
    }

    /**
     * Check if this event can be replayed (status = PENDING_REPLAY).
     *
     * @return true if replayable
     */
    public boolean isReplayable() {
        return status == DlqStatus.PENDING_REPLAY;
    }

    // ========== Enum ==========

    /**
     * Status of a Dead Letter Event.
     */
    public enum DlqStatus {
        /**
         * Event is waiting for operator decision (replay or ignore).
         */
        PENDING_REPLAY,

        /**
         * Event was successfully replayed and reprocessed.
         */
        REPLAYED,

        /**
         * Event was marked as non-actionable by an operator.
         */
        IGNORED
    }
}
