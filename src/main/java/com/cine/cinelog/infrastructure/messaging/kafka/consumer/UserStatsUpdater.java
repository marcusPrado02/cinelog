package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.features.readmodels.persistence.entity.UserStatsEntity;
import com.cine.cinelog.features.readmodels.repository.UserStatsRepository;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Consumidor Kafka que atualiza user_stats (CQRS Read Model).
 *
 * <p>
 * Este consumer implementa eventual consistency: ao receber eventos de
 * WatchEntry,
 * atualiza de forma assíncrona as estatísticas agregadas do usuário.
 *
 * <p>
 * <strong>Eventos processados:</strong>
 * <ul>
 * <li>WatchEntryCreatedEvent: incrementa total_watched, recalcula
 * avg_rating</li>
 * </ul>
 *
 * <p>
 * <strong>Idempotência:</strong> Garantida pelo Inbox Pattern (verifica eventId
 * antes de processar).
 *
 * <p>
 * <strong>Validação:</strong> Usa {@link EventEnvelopeValidator} (PR1) para
 * validar envelope.
 *
 * @since 1.0 (PR6)
 * @see UserStatsEntity
 * @see EventEnvelopeValidator
 */
@Component
public class UserStatsUpdater extends BaseKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserStatsUpdater.class);

    private final UserStatsRepository userStatsRepository;
    private final InboxEventRepository inboxRepository;
    private final EventEnvelopeValidator envelopeValidator;
    private final ObjectMapper objectMapper;
    private final WatchEntryJpaRepository watchEntryRepository;

    public UserStatsUpdater(
            UserStatsRepository userStatsRepository,
            InboxEventRepository inboxRepository,
            EventEnvelopeValidator envelopeValidator,
            ObjectMapper objectMapper,
            WatchEntryJpaRepository watchEntryRepository) {
        this.userStatsRepository = userStatsRepository;
        this.inboxRepository = inboxRepository;
        this.envelopeValidator = envelopeValidator;
        this.objectMapper = objectMapper;
        this.watchEntryRepository = watchEntryRepository;
    }

    @KafkaListener(topics = "${kafka.topics.watch-entry-created}", groupId = "${kafka.consumer.group-id}-user-stats", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleWatchEntryCreated(
            @Payload String message,
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {

        setupMDC(record);

        try {
            log.debug("UserStatsUpdater recebeu mensagem do tópico: {}", record.topic());

            // Deserializar envelope
            EventEnvelope<?> envelope = objectMapper.readValue(message, EventEnvelope.class);

            // Validar envelope (PR1)
            envelopeValidator.validate(envelope);

            // Idempotência: verificar se já processado
            UUID eventId = envelope.getEventId();
            if (inboxRepository.existsByEventId(eventId)) {
                log.info("Evento já processado (duplicate). EventId: {}. SKIP.", eventId);
                ack.acknowledge();
                return;
            }

            // Deserializar payload
            WatchEntryCreatedEvent event = objectMapper.convertValue(
                    envelope.getPayload(),
                    WatchEntryCreatedEvent.class);

            // Atualizar user_stats
            updateUserStats(event);

            // Registrar na inbox (garantir exatamente-uma-vez)
            InboxEventEntity inboxEvent = new InboxEventEntity(
                    eventId,
                    "UserStatsUpdater",
                    envelope.getType(),
                    event.userId().toString(),
                    java.time.Instant.now(),
                    message);
            inboxRepository.save(inboxEvent);

            log.info("user_stats atualizado com sucesso. UserId: {}, EventId: {}",
                    event.userId(), eventId);

            // Commit manual do offset
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Erro ao processar evento WatchEntryCreated no UserStatsUpdater", e);
            // Não dar ack -> Kafka vai redelivar a mensagem
            throw new RuntimeException("Falha ao processar evento", e);
        } finally {
            cleanupMDC();
        }
    }

    /**
     * Atualiza ou cria registro de user_stats para o evento recebido.
     *
     * <p>
     * Lógica:
     * 1. Busca ou cria UserStatsEntity
     * 2. Incrementa total_watched
     * 3. Incrementa total_movies OU total_series (baseado em episodeId)
     * 4. Recalcula avg_rating (se event tiver rating)
     * 5. Atualiza last_watched_at
     *
     * @param event evento WatchEntryCreated
     */
    private void updateUserStats(WatchEntryCreatedEvent event) {
        Long userId = event.userId();

        // Buscar ou criar stats
        UserStatsEntity stats = userStatsRepository.findById(userId)
                .orElseGet(() -> {
                    log.debug("Criando novo user_stats para userId={}", userId);
                    return new UserStatsEntity(userId);
                });

        // Incrementar contadores
        stats.incrementWatched();

        if (event.episodeId() != null) {
            stats.incrementSeries();
            log.debug("Série detectada. total_series={}", stats.getTotalSeries());
        } else {
            stats.incrementMovies();
            log.debug("Filme detectado. total_movies={}", stats.getTotalMovies());
        }

        // Recalcular avg_rating (se houver rating no evento)
        if (event.rating() != null) {
            recalculateAverageRating(stats, userId);
        }

        // Atualizar last_watched_at
        if (event.watchedAt() != null) {
            LocalDate watchedAt = event.watchedAt();
            if (stats.getLastWatchedAt() == null || watchedAt.isAfter(stats.getLastWatchedAt())) {
                stats.setLastWatchedAt(watchedAt);
            }
        }

        userStatsRepository.save(stats);

        log.debug("Stats atualizados: totalWatched={}, avgRating={}, lastWatchedAt={}",
                stats.getTotalWatched(), stats.getAvgRating(), stats.getLastWatchedAt());
    }

    /**
     * Recalcula a média de rating do usuário consultando watch_entry.
     *
     * <p>
     * Esta é uma operação custosa, mas necessária para manter consistência.
     * Em produção, considerar usar agregação incremental ou job batch.
     *
     * @param stats  entidade UserStats a ser atualizada
     * @param userId ID do usuário
     */
    private void recalculateAverageRating(UserStatsEntity stats, Long userId) {
        watchEntryRepository.averageRatingByUserId(userId).ifPresent(avg -> {
            stats.setAvgRating(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
            log.debug("avg_rating recalculado para userId={}: {}", userId, avg);
        });
    }
}
