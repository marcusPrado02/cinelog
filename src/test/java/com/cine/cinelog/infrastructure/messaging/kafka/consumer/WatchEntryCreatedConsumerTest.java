package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.core.application.services.DeadLetterService;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WatchEntryCreatedConsumer}.
 * <p>
 * Uses a real {@link ObjectMapper} (with JavaTimeModule) to construct valid
 * JSON messages, while all Spring/Kafka collaborators are mocked.
 * <p>
 * Covers:
 * - Valid v1 event → processBusinessLogic executed + cache evicted + ack committed
 * - Unsupported version → sendToDLQ called + ack committed (no retry)
 * - Duplicate event (inbox already has eventId) → skip + ack committed
 * - Invalid JSON envelope → sendToDLQ (InvalidEnvelopeException path)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WatchEntryCreatedConsumer Unit Tests")
class WatchEntryCreatedConsumerTest {

    @Mock
    private InboxEventRepository inboxRepository;

    @Mock
    private EventEnvelopeValidator envelopeValidator;

    @Mock
    private DeadLetterService deadLetterService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache userStatsCache;

    @Mock
    private Acknowledgment acknowledgment;

    private WatchEntryCreatedConsumer consumer;
    private ObjectMapper realMapper;

    @BeforeEach
    void setUp() {
        // Use real ObjectMapper so JSON deserialization works naturally in tests
        realMapper = new ObjectMapper();
        realMapper.registerModule(new JavaTimeModule());
        // mirror Spring Boot's default: don't fail on unknown properties
        realMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        consumer = new WatchEntryCreatedConsumer(
                inboxRepository, realMapper, envelopeValidator, deadLetterService, cacheManager);
    }

    // ========== Happy Path ==========

    @Test
    @DisplayName("Should process valid v1 event, evict cache, and commit ack")
    void shouldProcessValidEventAndEvictCache() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L, 10L, 100L, null, LocalDate.now(), new BigDecimal("8.0"));

        String envelopeJson = buildEnvelopeJson(eventId, event, 1);
        ConsumerRecord<String, String> record = buildRecord(envelopeJson);

        when(envelopeValidator.validate(any())).thenReturn(EventEnvelopeValidator.ValidationResult.valid());
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(cacheManager.getCache("userStats")).thenReturn(userStatsCache);
        when(inboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        consumer.consume(record, acknowledgment);

        // Then
        verify(cacheManager).getCache("userStats");
        verify(userStatsCache).evict(10L);
        verify(inboxRepository).save(any());
        verify(acknowledgment).acknowledge();
        verify(deadLetterService, never()).persistRawFailedEvent(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should evict cache using userId from payload")
    void shouldEvictCacheWithCorrectUserId() throws Exception {
        // Given
        UUID eventId = UUID.randomUUID();
        Long userId = 42L;
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                5L, userId, 200L, null, LocalDate.now(), null);

        String envelopeJson = buildEnvelopeJson(eventId, event, 1);
        ConsumerRecord<String, String> record = buildRecord(envelopeJson);

        when(envelopeValidator.validate(any())).thenReturn(EventEnvelopeValidator.ValidationResult.valid());
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(cacheManager.getCache("userStats")).thenReturn(userStatsCache);
        when(inboxRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        consumer.consume(record, acknowledgment);

        // Then — cache evicted with the correct userId
        verify(userStatsCache).evict(userId);
    }

    // ========== Unsupported Version ==========

    @Test
    @DisplayName("Should send to DLQ and ack when event version is unsupported")
    void shouldSendToDlqForUnsupportedVersion() throws Exception {
        // Given — envelope with version=2 (only v1 is supported)
        UUID eventId = UUID.randomUUID();
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L, 10L, 100L, null, LocalDate.now(), null);

        String envelopeJson = buildEnvelopeJson(eventId, event, 2);
        ConsumerRecord<String, String> record = buildRecord(envelopeJson);

        when(envelopeValidator.validate(any())).thenReturn(EventEnvelopeValidator.ValidationResult.valid());

        // When
        consumer.consume(record, acknowledgment);

        // Then — DLQ triggered, ack committed, inbox NOT saved
        verify(deadLetterService).persistRawFailedEvent(
                anyString(), anyString(), eq(envelopeJson), contains("Unsupported event version"));
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(inboxRepository);
    }

    // ========== Idempotency ==========

    @Test
    @DisplayName("Should skip duplicate event already recorded in inbox and ack")
    void shouldSkipDuplicateEventAndAck() throws Exception {
        // Given — eventId already in inbox
        UUID eventId = UUID.randomUUID();
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L, 10L, 100L, null, LocalDate.now(), null);

        String envelopeJson = buildEnvelopeJson(eventId, event, 1);
        ConsumerRecord<String, String> record = buildRecord(envelopeJson);

        when(envelopeValidator.validate(any())).thenReturn(EventEnvelopeValidator.ValidationResult.valid());
        when(inboxRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        consumer.consume(record, acknowledgment);

        // Then — no cache eviction, no inbox write, ack committed
        verifyNoInteractions(cacheManager);
        verify(inboxRepository, never()).save(any());
        verify(acknowledgment).acknowledge();
        verifyNoInteractions(deadLetterService);
    }

    // ========== Invalid Envelope ==========

    @Test
    @DisplayName("Should send to DLQ and ack when JSON envelope is malformed")
    void shouldSendToDlqForMalformedJson() {
        // Given — completely invalid JSON
        String invalidJson = "NOT_VALID_JSON";
        ConsumerRecord<String, String> record = buildRecord(invalidJson);

        // When
        consumer.consume(record, acknowledgment);

        // Then — InvalidEnvelopeException → DLQ + ack (no retry for poison pill)
        verify(deadLetterService).persistRawFailedEvent(
                anyString(), anyString(), eq(invalidJson), anyString());
        verify(acknowledgment).acknowledge();
    }

    // ========== Helpers ==========

    private String buildEnvelopeJson(UUID eventId, WatchEntryCreatedEvent payload, int version) throws Exception {
        EventEnvelope<WatchEntryCreatedEvent> envelope = EventEnvelope.<WatchEntryCreatedEvent>builder()
                .eventId(eventId)
                .type(WatchEntryCreatedEvent.EVENT_TYPE)
                .version(version)
                .occurredAt(Instant.now())
                .producer("test")
                .payload(payload)
                .build();
        return realMapper.writeValueAsString(envelope);
    }

    private ConsumerRecord<String, String> buildRecord(String value) {
        return new ConsumerRecord<>("cinelog.watchentry.created.v1", 0, 0L, "key", value);
    }
}
