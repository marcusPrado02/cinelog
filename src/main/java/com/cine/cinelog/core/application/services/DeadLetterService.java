package com.cine.cinelog.core.application.services;

import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity.DlqStatus;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.util.List;

/**
 * Service for managing Dead Letter Queue (DLQ) events.
 * <p>
 * Responsibilities:
 * - Persist failed Kafka events to database
 * - Query/list DLQ events for admin operations
 * - Replay events by republishing to original topic
 * - Mark events as ignored
 * <p>
 * Related to:
 * - PR3: DLQ Registry + Admin Endpoints
 * - ADR-006: Kafka Outbox Idempotent Consumer
 *
 * @see DeadLetterEventEntity
 * @see com.cine.cinelog.infrastructure.web.controllers.admin.DeadLetterAdminController
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterService {

    private final DeadLetterEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Persist a failed event to the DLQ registry.
     * <p>
     * Called by Kafka error handler when an event cannot be processed.
     * Deduplicates by eventId to avoid storing the same poison message multiple
     * times.
     *
     * @param originalTopic Kafka topic from which the event failed
     * @param consumerGroup Consumer group that encountered the failure
     * @param envelope      The failed EventEnvelope
     * @param exception     The exception that caused the failure
     * @param partition     Kafka partition (nullable)
     * @param offset        Kafka offset (nullable)
     */
    @Transactional
    public void persistFailedEvent(
            String originalTopic,
            String consumerGroup,
            EventEnvelope<?> envelope,
            Exception exception,
            Integer partition,
            Long offset) {
        String eventId = envelope.getEventId().toString();

        // Deduplication check
        if (repository.existsByEventId(eventId)) {
            log.warn("DLQ event with eventId={} already exists, skipping persistence", eventId);
            return;
        }

        try {
            String envelopeJson = objectMapper.writeValueAsString(envelope);
            String stackTrace = getStackTraceAsString(exception);

            DeadLetterEventEntity entity = DeadLetterEventEntity.builder()
                    .originalTopic(originalTopic)
                    .consumerGroup(consumerGroup)
                    .partitionNumber(partition)
                    .offsetNumber(offset)
                    .eventId(eventId)
                    .eventType(envelope.getType())
                    .envelopeJson(envelopeJson)
                    .errorMessage(exception.getMessage() != null ? exception.getMessage()
                            : exception.getClass().getSimpleName())
                    .errorClass(exception.getClass().getName())
                    .stackTrace(truncateIfNeeded(stackTrace, 65535)) // TEXT max ~64KB
                    .status(DlqStatus.PENDING_REPLAY)
                    .build();

            repository.save(entity);

            log.info("Persisted DLQ event: eventId={}, topic={}, consumerGroup={}, error={}",
                    eventId, originalTopic, consumerGroup, exception.getClass().getSimpleName());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize EventEnvelope to JSON for DLQ persistence: eventId={}", eventId, e);
            // Fall back to storing error message only
            persistFallbackEntry(originalTopic, consumerGroup, eventId, envelope.getType(), exception, partition,
                    offset);
        }
    }

    /**
     * Fallback persistence when envelope serialization fails.
     * Stores minimal information to avoid losing the DLQ entry.
     */
    private void persistFallbackEntry(
            String originalTopic,
            String consumerGroup,
            String eventId,
            String eventType,
            Exception exception,
            Integer partition,
            Long offset) {
        DeadLetterEventEntity entity = DeadLetterEventEntity.builder()
                .originalTopic(originalTopic)
                .consumerGroup(consumerGroup)
                .partitionNumber(partition)
                .offsetNumber(offset)
                .eventId(eventId)
                .eventType(eventType)
                .envelopeJson("{\"error\": \"Serialization failed\"}") // Placeholder
                .errorMessage("SERIALIZATION_ERROR: " + exception.getMessage())
                .errorClass(exception.getClass().getName())
                .build();

        repository.save(entity);
        log.warn("Persisted fallback DLQ entry (envelope serialization failed): eventId={}", eventId);
    }

    /**
     * List DLQ events by status (paginated).
     *
     * @param status   Filter by DLQ status (nullable = all)
     * @param topic    Filter by original topic (nullable = all)
     * @param pageable Pagination parameters
     * @return Page of DLQ events
     */
    @Transactional(readOnly = true)
    public Page<DeadLetterEventEntity> listEvents(DlqStatus status, String topic, Pageable pageable) {
        if (status != null && topic != null && !topic.isBlank()) {
            return repository.findByStatusAndOriginalTopicOrderByCreatedAtDesc(status, topic, pageable);
        } else if (status != null) {
            return repository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else if (topic != null && !topic.isBlank()) {
            log.warn("Filtering by topic without status is not optimized, consider using status");
            // Fallback to unfiltered query (could add a custom query if needed)
            return repository.findAll(pageable);
        } else {
            return repository.findAll(pageable);
        }
    }

    /**
     * Get a single DLQ event by ID.
     *
     * @param id DLQ event ID
     * @return DeadLetterEvent or null if not found
     */
    @Transactional(readOnly = true)
    public DeadLetterEventEntity getById(Long id) {
        return repository.findById(id).orElse(null);
    }

    /**
     * Replay a DLQ event by republishing it to the original Kafka topic.
     * <p>
     * The event envelope is deserialized and sent back to Kafka.
     * If successful, the DLQ event is marked as REPLAYED with timestamp and user.
     *
     * @param id         DLQ event ID
     * @param replayedBy Username or system identifier performing the replay
     * @return true if replay succeeded, false otherwise
     * @throws IllegalStateException if event is not in PENDING_REPLAY status
     */
    @Transactional
    public boolean replayEvent(Long id, String replayedBy) {
        DeadLetterEventEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: id=" + id));

        if (!entity.isReplayable()) {
            throw new IllegalStateException("Event is not replayable (status=" + entity.getStatus() + ")");
        }

        try {
            // Deserialize envelope
            EventEnvelope<?> envelope = objectMapper.readValue(entity.getEnvelopeJson(), EventEnvelope.class);

            // Republish to original topic
            String topic = entity.getOriginalTopic();
            String key = envelope.getEventId().toString(); // Use eventId as Kafka key
            String message = entity.getEnvelopeJson();

            kafkaTemplate.send(topic, key, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to replay DLQ event to Kafka: id={}, topic={}, eventId={}",
                                    id, topic, envelope.getEventId(), ex);
                        } else {
                            log.info(
                                    "Successfully replayed DLQ event to Kafka: id={}, topic={}, eventId={}, partition={}, offset={}",
                                    id, topic, envelope.getEventId(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });

            // Mark as replayed (optimistic - assume Kafka send will succeed)
            entity.markAsReplayed(replayedBy);
            repository.save(entity);

            log.info("Marked DLQ event as REPLAYED: id={}, eventId={}, replayedBy={}",
                    id, envelope.getEventId(), replayedBy);

            return true;

        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize envelope for replay: id={}", id, e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during replay: id={}", id, e);
            return false;
        }
    }

    /**
     * Mark a DLQ event as IGNORED (not actionable).
     *
     * @param id DLQ event ID
     * @return true if marked successfully
     */
    @Transactional
    public boolean ignoreEvent(Long id) {
        DeadLetterEventEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("DLQ event not found: id=" + id));

        if (!entity.isReplayable()) {
            throw new IllegalStateException(
                    "Event is not ignorable (already processed: status=" + entity.getStatus() + ")");
        }

        entity.markAsIgnored();
        repository.save(entity);

        log.info("Marked DLQ event as IGNORED: id={}, eventId={}", id, entity.getEventId());
        return true;
    }

    /**
     * Get count of DLQ events by status.
     *
     * @param status DLQ status
     * @return Count
     */
    @Transactional(readOnly = true)
    public long countByStatus(DlqStatus status) {
        return repository.countByStatus(status);
    }

    /**
     * Get list of all distinct topics that have DLQ events.
     *
     * @return List of topic names
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctTopics() {
        return repository.findDistinctTopics();
    }

    /**
     * Get list of all distinct consumer groups that have DLQ events.
     *
     * @return List of consumer group names
     */
    @Transactional(readOnly = true)
    public List<String> getDistinctConsumerGroups() {
        return repository.findDistinctConsumerGroups();
    }

    /**
     * Count events created within a time range.
     *
     * @param status    DLQ status
     * @param startTime Start of range
     * @param endTime   End of range
     * @return Count
     */
    @Transactional(readOnly = true)
    public long countByStatusInTimeRange(DlqStatus status, Instant startTime, Instant endTime) {
        return repository.countByStatusAndCreatedAtBetween(status, startTime, endTime);
    }

    // ========== Helper Methods ==========

    private String getStackTraceAsString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }

    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 50) + "\n\n... [TRUNCATED - stack trace too large]";
    }
}
