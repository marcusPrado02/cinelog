package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.features.readmodels.persistence.entity.MediaPopularityEntity;
import com.cine.cinelog.features.readmodels.repository.MediaPopularityRepository;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MediaPopularityUpdater}.
 * <p>
 * Covers:
 * - First watch (new entity): watch_count=1, (no rating) avgRating=null
 * - First watch with rating: ratings_count=1, avg=rating
 * - Second watch with rating: incremental avg formula verified
 * - Idempotency: duplicate skipped
 * - Event with no mediaId nor episodeId: ignored gracefully
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaPopularityUpdater Unit Tests")
class MediaPopularityUpdaterTest {

    @Mock
    private MediaPopularityRepository mediaPopularityRepository;

    @Mock
    private InboxEventRepository inboxRepository;

    @Mock
    private EventEnvelopeValidator envelopeValidator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private MediaPopularityUpdater updater;

    private UUID eventId;
    private ConsumerRecord<String, String> record;
    private static final String DUMMY_MSG = "{}";

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        record = new ConsumerRecord<>("watch-entry-created", 0, 0L, "key", DUMMY_MSG);
    }

    // ========== First Watch (no rating) ==========

    @Test
    @DisplayName("Should create new MediaPopularityEntity and increment watch_count on first watch")
    void shouldCreateEntityOnFirstWatch() throws Exception {
        // Given
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, 100L, null,
                LocalDate.now(), null);

        setupMocks(event);
        when(mediaPopularityRepository.findById(100L)).thenReturn(Optional.empty());
        when(mediaPopularityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MSG, record, acknowledgment);

        // Then
        ArgumentCaptor<MediaPopularityEntity> captor = ArgumentCaptor.forClass(MediaPopularityEntity.class);
        verify(mediaPopularityRepository).save(captor.capture());
        MediaPopularityEntity saved = captor.getValue();

        assertThat(saved.getMediaId()).isEqualTo(100L);
        assertThat(saved.getWatchCount()).isEqualTo(1L);
        assertThat(saved.getRatingsCount()).isEqualTo(0L);
        assertThat(saved.getAvgRating()).isNull();
        verify(acknowledgment).acknowledge();
    }

    // ========== First Watch with Rating ==========

    @Test
    @DisplayName("Should set avgRating equal to rating when first rating arrives")
    void shouldSetAvgRatingToFirstRating() throws Exception {
        // Given
        BigDecimal rating = new BigDecimal("9.0");
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, 100L, null,
                LocalDate.now(), rating);

        setupMocks(event);
        // Entity with no prior ratings
        MediaPopularityEntity existing = new MediaPopularityEntity(100L);
        when(mediaPopularityRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(mediaPopularityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MSG, record, acknowledgment);

        // Then
        // After incrementRatingsCount: count=1
        // new_avg = ((0 * (1-1)) + 9.0) / 1 = 9.0
        ArgumentCaptor<MediaPopularityEntity> captor = ArgumentCaptor.forClass(MediaPopularityEntity.class);
        verify(mediaPopularityRepository).save(captor.capture());
        MediaPopularityEntity saved = captor.getValue();

        assertThat(saved.getRatingsCount()).isEqualTo(1L);
        assertThat(saved.getAvgRating()).isEqualByComparingTo(new BigDecimal("9.00"));
    }

    // ========== Incremental Average ==========

    @Test
    @DisplayName("Should apply incremental avg formula: new_avg = ((old_avg * (n-1)) + new_rating) / n")
    void shouldApplyIncrementalAvgFormula() throws Exception {
        // Given — existing entity with 2 ratings: avg=7.0
        BigDecimal newRating = new BigDecimal("9.0");
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, 100L, null,
                LocalDate.now(), newRating);

        setupMocks(event);

        // Simulates entity BEFORE incrementRatingsCount is called:
        // ratingsCount=2, avgRating=7.0 (so after increment: count=3)
        MediaPopularityEntity existing = new MediaPopularityEntity(100L);
        existing.setRatingsCount(2L);
        existing.setAvgRating(new BigDecimal("7.00"));
        when(mediaPopularityRepository.findById(100L)).thenReturn(Optional.of(existing));
        when(mediaPopularityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MSG, record, acknowledgment);

        // Then: new_avg = ((7.0 * (3-1)) + 9.0) / 3 = (14.0 + 9.0) / 3 = 23/3 ≈ 7.67
        ArgumentCaptor<MediaPopularityEntity> captor = ArgumentCaptor.forClass(MediaPopularityEntity.class);
        verify(mediaPopularityRepository).save(captor.capture());
        MediaPopularityEntity saved = captor.getValue();

        assertThat(saved.getRatingsCount()).isEqualTo(3L);
        assertThat(saved.getAvgRating()).isEqualByComparingTo(new BigDecimal("7.67"));
    }

    // ========== Idempotency ==========

    @Test
    @DisplayName("Should skip duplicate events (inbox already contains eventId)")
    void shouldSkipDuplicateEvent() throws Exception {
        // Given
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, 100L, null,
                LocalDate.now(), null);
        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MSG, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        updater.handleWatchEntryCreated(DUMMY_MSG, record, acknowledgment);

        // Then
        verifyNoInteractions(mediaPopularityRepository);
        verify(acknowledgment).acknowledge();
    }

    // ========== Episode Route ==========

    @Test
    @DisplayName("Should use episodeId when mediaId is null")
    void shouldUseEpisodeIdWhenMediaIdIsNull() throws Exception {
        // Given — TV episode: mediaId=null, episodeId=200L
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, null, 200L,
                LocalDate.now(), null);

        setupMocks(event);
        when(mediaPopularityRepository.findById(200L)).thenReturn(Optional.empty());
        when(mediaPopularityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MSG, record, acknowledgment);

        // Then — popularity tracked by episodeId=200
        verify(mediaPopularityRepository).findById(200L);
    }

    // ========== Helpers ==========

    private void setupMocks(WatchEntryCreatedEvent event) throws Exception {
        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MSG, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(objectMapper.convertValue(any(), eq(WatchEntryCreatedEvent.class))).thenReturn(event);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
    }

    @SuppressWarnings("unchecked")
    private <T> EventEnvelope<T> buildEnvelope(UUID id, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(id)
                .type(WatchEntryCreatedEvent.EVENT_TYPE)
                .version(1)
                .occurredAt(Instant.now())
                .producer("test")
                .payload(payload)
                .build();
    }
}
