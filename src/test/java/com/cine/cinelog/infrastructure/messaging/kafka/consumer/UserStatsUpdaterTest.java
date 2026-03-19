package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.features.readmodels.persistence.entity.UserStatsEntity;
import com.cine.cinelog.features.readmodels.repository.UserStatsRepository;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
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
 * Unit tests for {@link UserStatsUpdater}.
 * <p>
 * Covers:
 * - New movie watched: total_watched + total_movies incremented
 * - New episode watched (series): total_watched + total_series incremented
 * - Event with rating: avg_rating recalculated via DB query
 * - Idempotency: duplicate event (already in inbox) is skipped
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserStatsUpdater Unit Tests")
class UserStatsUpdaterTest {

    @Mock
    private UserStatsRepository userStatsRepository;

    @Mock
    private InboxEventRepository inboxRepository;

    @Mock
    private EventEnvelopeValidator envelopeValidator;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WatchEntryJpaRepository watchEntryRepository;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private UserStatsUpdater updater;

    private UUID eventId;
    private ConsumerRecord<String, String> record;
    private static final String DUMMY_MESSAGE = "{\"eventId\":\"...\"}";

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        Headers headers = new RecordHeaders();
        record = new ConsumerRecord<>("watch-entry-created", 0, 0L, "key", DUMMY_MESSAGE);
    }

    // ========== Movie Watched ==========

    @Test
    @DisplayName("Should increment total_watched and total_movies for a new movie")
    void shouldIncrementMovieCountersForNewMovie() throws Exception {
        // Given
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 1L, 10L, 100L, null,
                LocalDate.now(), null);

        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MESSAGE, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(objectMapper.convertValue(any(), eq(WatchEntryCreatedEvent.class))).thenReturn(event);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(userStatsRepository.findById(10L)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MESSAGE, record, acknowledgment);

        // Then
        ArgumentCaptor<UserStatsEntity> captor = ArgumentCaptor.forClass(UserStatsEntity.class);
        verify(userStatsRepository).save(captor.capture());
        UserStatsEntity saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(10L);
        assertThat(saved.getTotalWatched()).isEqualTo(1L);
        assertThat(saved.getTotalMovies()).isEqualTo(1L);
        assertThat(saved.getTotalSeries()).isEqualTo(0L);
        verify(acknowledgment).acknowledge();
    }

    // ========== Series/Episode Watched ==========

    @Test
    @DisplayName("Should increment total_series when episodeId is present")
    void shouldIncrementSeriesCounterForEpisode() throws Exception {
        // Given
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 2L, 10L, null, 200L,
                LocalDate.now(), null);

        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MESSAGE, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(objectMapper.convertValue(any(), eq(WatchEntryCreatedEvent.class))).thenReturn(event);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(userStatsRepository.findById(10L)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MESSAGE, record, acknowledgment);

        // Then
        ArgumentCaptor<UserStatsEntity> captor = ArgumentCaptor.forClass(UserStatsEntity.class);
        verify(userStatsRepository).save(captor.capture());
        UserStatsEntity saved = captor.getValue();

        assertThat(saved.getTotalSeries()).isEqualTo(1L);
        assertThat(saved.getTotalMovies()).isEqualTo(0L);
    }

    // ========== Rating Recalculation ==========

    @Test
    @DisplayName("Should recalculate avg_rating via DB query when rating is present in event")
    void shouldRecalculateAvgRatingWhenRatingPresent() throws Exception {
        // Given
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 3L, 10L, 100L, null,
                LocalDate.now(), new BigDecimal("8.5"));

        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MESSAGE, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(objectMapper.convertValue(any(), eq(WatchEntryCreatedEvent.class))).thenReturn(event);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);

        UserStatsEntity existingStats = new UserStatsEntity(10L);
        when(userStatsRepository.findById(10L)).thenReturn(Optional.of(existingStats));
        when(watchEntryRepository.averageRatingByUserId(10L)).thenReturn(Optional.of(8.5));
        when(userStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MESSAGE, record, acknowledgment);

        // Then
        verify(watchEntryRepository).averageRatingByUserId(10L);
        ArgumentCaptor<UserStatsEntity> captor = ArgumentCaptor.forClass(UserStatsEntity.class);
        verify(userStatsRepository).save(captor.capture());
        assertThat(captor.getValue().getAvgRating())
                .isEqualByComparingTo(new BigDecimal("8.50"));
    }

    @Test
    @DisplayName("Should NOT recalculate avg_rating when event has no rating")
    void shouldNotRecalculateAvgRatingWhenNoRating() throws Exception {
        // Given — null rating
        WatchEntryCreatedEvent event = new WatchEntryCreatedEvent(
                eventId, Instant.now(), 4L, 10L, 100L, null,
                LocalDate.now(), null);

        EventEnvelope<?> envelope = buildEnvelope(eventId, event);
        when(objectMapper.readValue(DUMMY_MESSAGE, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(objectMapper.convertValue(any(), eq(WatchEntryCreatedEvent.class))).thenReturn(event);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(false);
        when(userStatsRepository.findById(10L)).thenReturn(Optional.empty());
        when(userStatsRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        updater.handleWatchEntryCreated(DUMMY_MESSAGE, record, acknowledgment);

        // Then
        verifyNoInteractions(watchEntryRepository);
    }

    // ========== Idempotency ==========

    @Test
    @DisplayName("Should skip duplicate events already recorded in inbox")
    void shouldSkipDuplicateEvent() throws Exception {
        // Given — event already in inbox
        EventEnvelope<?> envelope = buildEnvelope(eventId, "payload");
        when(objectMapper.readValue(DUMMY_MESSAGE, EventEnvelope.class)).thenReturn((EventEnvelope) envelope);
        when(inboxRepository.existsByEventId(eventId)).thenReturn(true);

        // When
        updater.handleWatchEntryCreated(DUMMY_MESSAGE, record, acknowledgment);

        // Then — no stats updated, but ack still committed
        verifyNoInteractions(userStatsRepository);
        verifyNoInteractions(watchEntryRepository);
        verify(acknowledgment).acknowledge();
    }

    // ========== Helpers ==========

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
