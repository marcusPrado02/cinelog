package com.cine.cinelog.core.application.services;

import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventEntity.DlqStatus;
import com.cine.cinelog.infrastructure.persistence.dlq.DeadLetterEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DeadLetterService}.
 * <p>
 * Tests cover:
 * - Persisting failed events
 * - Deduplication by eventId
 * - Listing/filtering DLQ events
 * - Replay functionality
 * - Ignore functionality
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeadLetterService Unit Tests")
class DeadLetterServiceTest {

    @Mock
    private DeadLetterEventRepository repository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DeadLetterService service;

    private EventEnvelope<String> sampleEnvelope;
    private DeadLetterEventEntity sampleEntity;

    @BeforeEach
    void setUp() {
        sampleEnvelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("test-service")
                .payload("test payload")
                .build();

        sampleEntity = DeadLetterEventEntity.builder()
                .id(1L)
                .originalTopic("test-topic")
                .consumerGroup("test-group")
                .eventId(sampleEnvelope.getEventId().toString())
                .eventType("test_event")
                .envelopeJson("{\"eventId\":\"" + sampleEnvelope.getEventId() + "\"}")
                .errorMessage("Test error")
                .errorClass("TestException")
                .status(DlqStatus.PENDING_REPLAY)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    // ========== Persist Failed Event Tests ==========

    @Test
    @DisplayName("Should persist failed event successfully")
    void shouldPersistFailedEvent() throws JsonProcessingException {
        // Given
        String topic = "test-topic";
        String consumerGroup = "test-group";
        Exception exception = new RuntimeException("Kafka deserialization error");
        String envelopeJson = "{\"eventId\":\"123\"}";

        when(repository.existsByEventId(sampleEnvelope.getEventId().toString())).thenReturn(false);
        when(objectMapper.writeValueAsString(sampleEnvelope)).thenReturn(envelopeJson);

        // When
        service.persistFailedEvent(topic, consumerGroup, sampleEnvelope, exception, 0, 100L);

        // Then
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(repository).save(captor.capture());

        DeadLetterEventEntity saved = captor.getValue();
        assertThat(saved.getOriginalTopic()).isEqualTo(topic);
        assertThat(saved.getConsumerGroup()).isEqualTo(consumerGroup);
        assertThat(saved.getEventId()).isEqualTo(sampleEnvelope.getEventId().toString());
        assertThat(saved.getEventType()).isEqualTo("test_event");
        assertThat(saved.getEnvelopeJson()).isEqualTo(envelopeJson);
        assertThat(saved.getErrorMessage()).isEqualTo("Kafka deserialization error");
        assertThat(saved.getErrorClass()).isEqualTo(RuntimeException.class.getName());
        assertThat(saved.getStatus()).isEqualTo(DlqStatus.PENDING_REPLAY);
        assertThat(saved.getPartitionNumber()).isEqualTo(0);
        assertThat(saved.getOffsetNumber()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Should skip persistence if eventId already exists (deduplication)")
    void shouldSkipPersistenceIfEventIdExists() throws JsonProcessingException {
        // Given
        when(repository.existsByEventId(sampleEnvelope.getEventId().toString())).thenReturn(true);

        // When
        service.persistFailedEvent("topic", "group", sampleEnvelope, new RuntimeException(), null, null);

        // Then
        verify(repository, never()).save(any());
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    @DisplayName("Should persist fallback entry when envelope serialization fails")
    void shouldPersistFallbackWhenSerializationFails() throws JsonProcessingException {
        // Given
        when(repository.existsByEventId(sampleEnvelope.getEventId().toString())).thenReturn(false);
        when(objectMapper.writeValueAsString(sampleEnvelope))
                .thenThrow(new JsonProcessingException("Serialization error") {
                });

        // When
        service.persistFailedEvent("topic", "group", sampleEnvelope, new RuntimeException("Original error"), null,
                null);

        // Then
        ArgumentCaptor<DeadLetterEventEntity> captor = ArgumentCaptor.forClass(DeadLetterEventEntity.class);
        verify(repository).save(captor.capture());

        DeadLetterEventEntity saved = captor.getValue();
        assertThat(saved.getEnvelopeJson()).isEqualTo("{\"error\": \"Serialization failed\"}");
        assertThat(saved.getErrorMessage()).contains("SERIALIZATION_ERROR");
    }

    // ========== List Events Tests ==========

    @Test
    @DisplayName("Should list events by status")
    void shouldListEventsByStatus() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<DeadLetterEventEntity> page = new PageImpl<>(List.of(sampleEntity));

        when(repository.findByStatusOrderByCreatedAtDesc(DlqStatus.PENDING_REPLAY, pageable)).thenReturn(page);

        // When
        Page<DeadLetterEventEntity> result = service.listEvents(DlqStatus.PENDING_REPLAY, null, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0)).isEqualTo(sampleEntity);
        verify(repository).findByStatusOrderByCreatedAtDesc(DlqStatus.PENDING_REPLAY, pageable);
    }

    @Test
    @DisplayName("Should list events by status and topic")
    void shouldListEventsByStatusAndTopic() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        Page<DeadLetterEventEntity> page = new PageImpl<>(List.of(sampleEntity));

        when(repository.findByStatusAndOriginalTopicOrderByCreatedAtDesc(
                DlqStatus.PENDING_REPLAY, "test-topic", pageable)).thenReturn(page);

        // When
        Page<DeadLetterEventEntity> result = service.listEvents(DlqStatus.PENDING_REPLAY, "test-topic", pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        verify(repository).findByStatusAndOriginalTopicOrderByCreatedAtDesc(
                DlqStatus.PENDING_REPLAY, "test-topic", pageable);
    }

    // ========== Get By ID Tests ==========

    @Test
    @DisplayName("Should get event by ID")
    void shouldGetEventById() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        // When
        DeadLetterEventEntity result = service.getById(1L);

        // Then
        assertThat(result).isEqualTo(sampleEntity);
    }

    @Test
    @DisplayName("Should return null when event not found")
    void shouldReturnNullWhenEventNotFound() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When
        DeadLetterEventEntity result = service.getById(999L);

        // Then
        assertThat(result).isNull();
    }

    // ========== Replay Event Tests ==========

    @Test
    @DisplayName("Should replay event successfully")
    @SuppressWarnings("unchecked")
    void shouldReplayEventSuccessfully() throws JsonProcessingException {
        // Given
        Long eventId = 1L;
        String replayedBy = "admin-user";

        when(repository.findById(eventId)).thenReturn(Optional.of(sampleEntity));
        when(objectMapper.readValue(anyString(), eq(EventEnvelope.class))).thenReturn(sampleEnvelope);
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));

        // When
        boolean result = service.replayEvent(eventId, replayedBy);

        // Then
        assertThat(result).isTrue();
        verify(kafkaTemplate).send(eq("test-topic"), eq(sampleEnvelope.getEventId().toString()), anyString());
        verify(repository).save(argThat(entity -> entity.getStatus() == DlqStatus.REPLAYED &&
                entity.getReplayedBy().equals(replayedBy) &&
                entity.getReplayedAt() != null));
    }

    @Test
    @DisplayName("Should throw exception when replaying non-existent event")
    void shouldThrowExceptionWhenReplayingNonExistentEvent() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.replayEvent(999L, "admin"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DLQ event not found");
    }

    @Test
    @DisplayName("Should throw exception when replaying already processed event")
    void shouldThrowExceptionWhenReplayingAlreadyProcessedEvent() {
        // Given
        sampleEntity.setStatus(DlqStatus.REPLAYED);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        // When/Then
        assertThatThrownBy(() -> service.replayEvent(1L, "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not replayable");
    }

    @Test
    @DisplayName("Should return false when deserialization fails during replay")
    void shouldReturnFalseWhenDeserializationFailsDuringReplay() throws JsonProcessingException {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));
        when(objectMapper.readValue(anyString(), eq(EventEnvelope.class)))
                .thenThrow(new JsonProcessingException("Deserialization error") {
                });

        // When
        boolean result = service.replayEvent(1L, "admin");

        // Then
        assertThat(result).isFalse();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    // ========== Ignore Event Tests ==========

    @Test
    @DisplayName("Should ignore event successfully")
    void shouldIgnoreEventSuccessfully() {
        // Given
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        // When
        boolean result = service.ignoreEvent(1L);

        // Then
        assertThat(result).isTrue();
        verify(repository).save(argThat(entity -> entity.getStatus() == DlqStatus.IGNORED));
    }

    @Test
    @DisplayName("Should throw exception when ignoring non-existent event")
    void shouldThrowExceptionWhenIgnoringNonExistentEvent() {
        // Given
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> service.ignoreEvent(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DLQ event not found");
    }

    @Test
    @DisplayName("Should throw exception when ignoring already processed event")
    void shouldThrowExceptionWhenIgnoringAlreadyProcessedEvent() {
        // Given
        sampleEntity.setStatus(DlqStatus.IGNORED);
        when(repository.findById(1L)).thenReturn(Optional.of(sampleEntity));

        // When/Then
        assertThatThrownBy(() -> service.ignoreEvent(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not ignorable");
    }

    // ========== Stats/Queries Tests ==========

    @Test
    @DisplayName("Should count events by status")
    void shouldCountEventsByStatus() {
        // Given
        when(repository.countByStatus(DlqStatus.PENDING_REPLAY)).thenReturn(15L);

        // When
        long count = service.countByStatus(DlqStatus.PENDING_REPLAY);

        // Then
        assertThat(count).isEqualTo(15L);
    }

    @Test
    @DisplayName("Should get distinct topics")
    void shouldGetDistinctTopics() {
        // Given
        List<String> topics = List.of("topic-1", "topic-2");
        when(repository.findDistinctTopics()).thenReturn(topics);

        // When
        List<String> result = service.getDistinctTopics();

        // Then
        assertThat(result).containsExactly("topic-1", "topic-2");
    }

    @Test
    @DisplayName("Should get distinct consumer groups")
    void shouldGetDistinctConsumerGroups() {
        // Given
        List<String> groups = List.of("group-1", "group-2");
        when(repository.findDistinctConsumerGroups()).thenReturn(groups);

        // When
        List<String> result = service.getDistinctConsumerGroups();

        // Then
        assertThat(result).containsExactly("group-1", "group-2");
    }

    @Test
    @DisplayName("Should count events in time range")
    void shouldCountEventsInTimeRange() {
        // Given
        Instant start = Instant.now().minusSeconds(3600);
        Instant end = Instant.now();
        when(repository.countByStatusAndCreatedAtBetween(DlqStatus.PENDING_REPLAY, start, end)).thenReturn(10L);

        // When
        long count = service.countByStatusInTimeRange(DlqStatus.PENDING_REPLAY, start, end);

        // Then
        assertThat(count).isEqualTo(10L);
    }
}
