package com.cine.cinelog.infrastructure.web.dto.admin;

import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity.DlqStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for detailed Dead Letter Queue event (includes full envelope and stack
 * trace).
 * <p>
 * Used for single event detail endpoint.
 * <p>
 * Related to:
 * - PR3: DLQ Registry + Admin Endpoints
 * - ADR-006: Kafka Outbox Idempotent Consumer
 *
 * @see com.cine.cinelog.infrastructure.web.controllers.admin.DeadLetterAdminController
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeadLetterEventDetailResponse {

    private Long id;
    private String originalTopic;
    private String consumerGroup;
    private Integer partitionNumber;
    private Long offsetNumber;
    private String eventId;
    private String eventType;
    private String envelopeJson; // Full envelope for investigation
    private String errorMessage;
    private String errorClass;
    private String stackTrace; // Full stack trace
    private DlqStatus status;
    private Instant replayedAt;
    private String replayedBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert entity to detailed response DTO (includes all fields).
     */
    public static DeadLetterEventDetailResponse fromEntity(DeadLetterEventEntity entity) {
        return DeadLetterEventDetailResponse.builder()
                .id(entity.getId())
                .originalTopic(entity.getOriginalTopic())
                .consumerGroup(entity.getConsumerGroup())
                .partitionNumber(entity.getPartitionNumber())
                .offsetNumber(entity.getOffsetNumber())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .envelopeJson(entity.getEnvelopeJson())
                .errorMessage(entity.getErrorMessage())
                .errorClass(entity.getErrorClass())
                .stackTrace(entity.getStackTrace())
                .status(entity.getStatus())
                .replayedAt(entity.getReplayedAt())
                .replayedBy(entity.getReplayedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
