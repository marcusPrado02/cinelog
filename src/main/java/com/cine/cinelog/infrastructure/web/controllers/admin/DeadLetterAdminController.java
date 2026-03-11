package com.cine.cinelog.infrastructure.web.controllers.admin;

import com.cine.cinelog.core.application.services.DeadLetterService;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity.DlqStatus;
import com.cine.cinelog.infrastructure.web.dto.admin.DeadLetterEventDetailResponse;
import com.cine.cinelog.infrastructure.web.dto.admin.DeadLetterEventResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * Admin REST Controller for Dead Letter Queue (DLQ) management.
 * <p>
 * **Security:** All endpoints require ADMIN or OPS role.
 * <p>
 * Endpoints:
 * - GET /admin/dlq - List DLQ events (paginated, filterable)
 * - GET /admin/dlq/{id} - Get single DLQ event detail
 * - POST /admin/dlq/{id}/replay - Replay event to original Kafka topic
 * - POST /admin/dlq/{id}/ignore - Mark event as ignored
 * - GET /admin/dlq/stats - Get DLQ statistics
 * - GET /admin/dlq/topics - Get list of topics with DLQ events
 * <p>
 * Related to:
 * - PR3: DLQ Registry + Admin Endpoints
 * - ADR-006: Kafka Outbox Idempotent Consumer
 *
 * @see DeadLetterService
 * @see DeadLetterEventEntity
 */
@RestController
@RequestMapping("/admin/dlq")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Dead Letter Queue", description = "DLQ event management (ADMIN/OPS only)")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAnyRole('ADMIN', 'OPS')") // Apply security to entire controller
public class DeadLetterAdminController {

    private final DeadLetterService deadLetterService;

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\n', ' ').replace('\r', ' ');
    }

    /**
     * List DLQ events with optional filters.
     * <p>
     * Query parameters:
     * - status: PENDING_REPLAY | REPLAYED | IGNORED (optional)
     * - topic: Filter by original topic (optional)
     * - page: Page number (default 0)
     * - size: Page size (default 20)
     * - sort: Sort by field (default: createdAt,desc)
     *
     * @param status   Filter by DLQ status
     * @param topic    Filter by original topic
     * @param pageable Pagination parameters
     * @return Page of DLQ events
     */
    @GetMapping
    @Operation(summary = "List DLQ events", description = "Retrieve paginated list of dead letter events with optional filters. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ events retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<Page<DeadLetterEventResponse>> listDlqEvents(
            @Parameter(description = "Filter by status (PENDING_REPLAY, REPLAYED, IGNORED)") @RequestParam(required = false) DlqStatus status,

            @Parameter(description = "Filter by original Kafka topic") @RequestParam(required = false) String topic,

            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        log.info("Admin DLQ list request: status={}, topic={}, page={}, size={}",
                status, sanitizeForLog(topic), pageable.getPageNumber(), pageable.getPageSize());

        Page<DeadLetterEventEntity> entities = deadLetterService.listEvents(status, topic, pageable);
        Page<DeadLetterEventResponse> response = entities.map(DeadLetterEventResponse::fromEntity);

        return ResponseEntity.ok(response);
    }

    /**
     * Get single DLQ event detail (includes full envelope and stack trace).
     *
     * @param id DLQ event ID
     * @return Detailed DLQ event
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get DLQ event detail", description = "Retrieve full details of a dead letter event, including envelope JSON and stack trace. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DLQ event found"),
            @ApiResponse(responseCode = "404", description = "DLQ event not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<DeadLetterEventDetailResponse> getDlqEventDetail(
            @Parameter(description = "DLQ event ID") @PathVariable Long id) {
        log.info("Admin DLQ detail request: id={}", id);

        DeadLetterEventEntity entity = deadLetterService.getById(id);
        if (entity == null) {
            log.warn("DLQ event not found: id={}", id);
            return ResponseEntity.notFound().build();
        }

        DeadLetterEventDetailResponse response = DeadLetterEventDetailResponse.fromEntity(entity);
        return ResponseEntity.ok(response);
    }

    /**
     * Replay a DLQ event by republishing it to the original Kafka topic.
     * <p>
     * The event status will be updated to REPLAYED.
     *
     * @param id        DLQ event ID
     * @param principal Authenticated user (auto-injected)
     * @return Success message
     */
    @PostMapping("/{id}/replay")
    @Operation(summary = "Replay DLQ event", description = "Republish a dead letter event to its original Kafka topic. Marks event as REPLAYED. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event replayed successfully"),
            @ApiResponse(responseCode = "400", description = "Event is not replayable (already REPLAYED or IGNORED)"),
            @ApiResponse(responseCode = "404", description = "DLQ event not found"),
            @ApiResponse(responseCode = "500", description = "Replay failed (Kafka error)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<Map<String, String>> replayEvent(
            @Parameter(description = "DLQ event ID") @PathVariable Long id,
            Principal principal) {
        String username = principal != null ? principal.getName() : "system";
        log.info("Admin DLQ replay request: id={}, replayedBy={}", id, username);

        try {
            boolean success = deadLetterService.replayEvent(id, username);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Event replayed successfully",
                        "id", id.toString(),
                        "replayedBy", username));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of(
                                "status", "error",
                                "message", "Failed to replay event (check logs)",
                                "id", id.toString()));
            }

        } catch (IllegalArgumentException e) {
            log.warn("DLQ replay failed - not found: id={}", id);
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.warn("DLQ replay failed - not replayable: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "id", id.toString()));
        }
    }

    /**
     * Mark a DLQ event as IGNORED (not actionable).
     *
     * @param id DLQ event ID
     * @return Success message
     */
    @PostMapping("/{id}/ignore")
    @Operation(summary = "Ignore DLQ event", description = "Mark a dead letter event as IGNORED (not actionable). Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event ignored successfully"),
            @ApiResponse(responseCode = "400", description = "Event is not ignorable (already processed)"),
            @ApiResponse(responseCode = "404", description = "DLQ event not found"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<Map<String, String>> ignoreEvent(
            @Parameter(description = "DLQ event ID") @PathVariable Long id) {
        log.info("Admin DLQ ignore request: id={}", id);

        try {
            deadLetterService.ignoreEvent(id);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Event marked as IGNORED",
                    "id", id.toString()));

        } catch (IllegalArgumentException e) {
            log.warn("DLQ ignore failed - not found: id={}", id);
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.warn("DLQ ignore failed - not ignorable: id={}, error={}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "status", "error",
                            "message", e.getMessage(),
                            "id", id.toString()));
        }
    }

    /**
     * Get DLQ statistics (counts by status).
     *
     * @return Stats map
     */
    @GetMapping("/stats")
    @Operation(summary = "Get DLQ statistics", description = "Retrieve counts of DLQ events by status. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<Map<String, Long>> getDlqStats() {
        log.info("Admin DLQ stats request");

        long pending = deadLetterService.countByStatus(DlqStatus.PENDING_REPLAY);
        long replayed = deadLetterService.countByStatus(DlqStatus.REPLAYED);
        long ignored = deadLetterService.countByStatus(DlqStatus.IGNORED);
        long total = pending + replayed + ignored;

        return ResponseEntity.ok(Map.of(
                "total", total,
                "pending", pending,
                "replayed", replayed,
                "ignored", ignored));
    }

    /**
     * Get list of all topics that have DLQ events.
     *
     * @return List of topic names
     */
    @GetMapping("/topics")
    @Operation(summary = "Get DLQ topics", description = "Retrieve list of all Kafka topics that have dead letter events. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Topics retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<List<String>> getDlqTopics() {
        log.info("Admin DLQ topics request");

        List<String> topics = deadLetterService.getDistinctTopics();
        return ResponseEntity.ok(topics);
    }

    /**
     * Get list of all consumer groups that have DLQ events.
     *
     * @return List of consumer group names
     */
    @GetMapping("/consumer-groups")
    @Operation(summary = "Get DLQ consumer groups", description = "Retrieve list of all consumer groups that have dead letter events. Requires ADMIN or OPS role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consumer groups retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Requires ADMIN or OPS role")
    })
    public ResponseEntity<List<String>> getDlqConsumerGroups() {
        log.info("Admin DLQ consumer groups request");

        List<String> consumerGroups = deadLetterService.getDistinctConsumerGroups();
        return ResponseEntity.ok(consumerGroups);
    }
}
