package com.cine.cinelog.infrastructure.web.dto.admin;

import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity.DlqStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for Dead Letter Queue events exposed via admin endpoints.
 * <p>
 * Excludes large fields like stackTrace by default (available via detail
 * endpoint).
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
public class DeadLetterEventResponse {

    private Long id;
    private String originalTopic;
    private String consumerGroup;
    private Integer partitionNumber;
    private Long offsetNumber;
    private String eventId;
    private String eventType;
    private String errorMessage;
    private String errorClass;
    private DlqStatus status;
    private Instant replayedAt;
    private String replayedBy;
    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Convert entity to response DTO (excludes envelopeJson and stackTrace).
     */
    public static DeadLetterEventResponse fromEntity(DeadLetterEventEntity entity) {
        return DeadLetterEventResponse.builder()
                .id(entity.getId())
                .originalTopic(entity.getOriginalTopic())
                .consumerGroup(entity.getConsumerGroup())
                .partitionNumber(entity.getPartitionNumber())
                .offsetNumber(entity.getOffsetNumber())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .errorMessage(truncateForList(entity.getErrorMessage()))
                .errorClass(entity.getErrorClass())
                .status(entity.getStatus())
                .replayedAt(entity.getReplayedAt())
                .replayedBy(entity.getReplayedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * Truncate error message for list view (first 200 chars).
     */
    private static String truncateForList(String message) {
        if (message == null) {
            return null;
        }
        if (message.length() <= 200) {
            return message;
        }
        return message.substring(0, 200) + "...";
    }
}
