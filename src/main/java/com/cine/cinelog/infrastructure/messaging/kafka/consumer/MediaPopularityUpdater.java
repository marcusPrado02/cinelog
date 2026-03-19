package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.features.readmodels.persistence.entity.MediaPopularityEntity;
import com.cine.cinelog.features.readmodels.repository.MediaPopularityRepository;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Consumidor Kafka que atualiza media_popularity (CQRS Read Model).
 *
 * <p>
 * Este consumer implementa eventual consistency: ao receber eventos de
 * WatchEntry,
 * atualiza de forma assíncrona as métricas de popularidade da mídia.
 *
 * <p>
 * <strong>Eventos processados:</strong>
 * <ul>
 * <li>WatchEntryCreatedEvent: incrementa watch_count, recalcula avg_rating</li>
 * </ul>
 *
 * <p>
 * <strong>Métricas atualizadas:</strong>
 * <ul>
 * <li>watch_count: total de visualizações</li>
 * <li>ratings_count: total de avaliações</li>
 * <li>avg_rating: média ponderada de ratings</li>
 * <li>last_watched_at: data/hora da última visualização</li>
 * </ul>
 *
 * <p>
 * <strong>Idempotência:</strong> Garantida pelo Inbox Pattern.
 *
 * @since 1.0 (PR6)
 * @see MediaPopularityEntity
 * @see EventEnvelopeValidator
 */
@Component
public class MediaPopularityUpdater extends BaseKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(MediaPopularityUpdater.class);

    private final MediaPopularityRepository mediaPopularityRepository;
    private final InboxEventRepository inboxRepository;
    private final EventEnvelopeValidator envelopeValidator;
    private final ObjectMapper objectMapper;

    public MediaPopularityUpdater(
            MediaPopularityRepository mediaPopularityRepository,
            InboxEventRepository inboxRepository,
            EventEnvelopeValidator envelopeValidator,
            ObjectMapper objectMapper) {
        this.mediaPopularityRepository = mediaPopularityRepository;
        this.inboxRepository = inboxRepository;
        this.envelopeValidator = envelopeValidator;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.watch-entry-created}", groupId = "${kafka.consumer.group-id}-media-popularity", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void handleWatchEntryCreated(
            @Payload String message,
            ConsumerRecord<String, String> record,
            Acknowledgment ack) {

        setupMDC(record);

        try {
            log.debug("MediaPopularityUpdater recebeu mensagem do tópico: {}", record.topic());

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

            // Atualizar media_popularity
            updateMediaPopularity(event);

            // Registrar na inbox (garantir exatamente-uma-vez)
            InboxEventEntity inboxEvent = new InboxEventEntity(
                    eventId,
                    "MediaPopularityUpdater",
                    envelope.getType(),
                    getAggregateId(event),
                    java.time.Instant.now(),
                    message);
            inboxRepository.save(inboxEvent);

            log.info("media_popularity atualizado com sucesso. MediaId: {}, EventId: {}",
                    event.mediaId(), eventId);

            // Commit manual do offset
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Erro ao processar evento WatchEntryCreated no MediaPopularityUpdater", e);
            throw new RuntimeException("Falha ao processar evento", e);
        } finally {
            cleanupMDC();
        }
    }

    /**
     * Atualiza ou cria registro de media_popularity para o evento recebido.
     *
     * <p>
     * Lógica:
     * 1. Busca ou cria MediaPopularityEntity (para mediaId OU episodeId)
     * 2. Incrementa watch_count
     * 3. Se evento tem rating: incrementa ratings_count e recalcula avg_rating
     * 4. Atualiza last_watched_at
     *
     * @param event evento WatchEntryCreated
     */
    private void updateMediaPopularity(WatchEntryCreatedEvent event) {
        // Determinar qual ID usar: mediaId ou episodeId
        Long targetMediaId = event.mediaId() != null ? event.mediaId() : event.episodeId();

        if (targetMediaId == null) {
            log.warn("Evento sem mediaId nem episodeId. Ignorando. EventId: {}", event.eventId());
            return;
        }

        // Buscar ou criar popularity
        MediaPopularityEntity popularity = mediaPopularityRepository.findById(targetMediaId)
                .orElseGet(() -> {
                    log.debug("Criando novo media_popularity para mediaId={}", targetMediaId);
                    return new MediaPopularityEntity(targetMediaId);
                });

        // Incrementar watch_count
        popularity.incrementWatchCount();

        // Atualizar rating (se houver)
        if (event.rating() != null) {
            popularity.incrementRatingsCount();

            // Avg incremental: new_avg = ((old_avg * (n-1)) + new_rating) / n
            BigDecimal oldAvg = popularity.getAvgRating() != null
                    ? popularity.getAvgRating()
                    : BigDecimal.ZERO;
            long count = popularity.getRatingsCount();
            BigDecimal newAvg = (oldAvg.multiply(BigDecimal.valueOf(count - 1))
                    .add(event.rating()))
                    .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            popularity.setAvgRating(newAvg);
            log.debug("avg_rating atualizado: {} → {} (rating={}, count={})",
                    oldAvg, newAvg, event.rating(), count);
        }

        // Atualizar last_watched_at
        LocalDateTime now = LocalDateTime.now();
        if (popularity.getLastWatchedAt() == null || now.isAfter(popularity.getLastWatchedAt())) {
            popularity.setLastWatchedAt(now);
        }

        mediaPopularityRepository.save(popularity);

        log.debug("Popularity atualizado: watchCount={}, ratingsCount={}, lastWatchedAt={}",
                popularity.getWatchCount(), popularity.getRatingsCount(), popularity.getLastWatchedAt());
    }

    /**
     * Retorna aggregateId para registrar na inbox.
     */
    private String getAggregateId(WatchEntryCreatedEvent event) {
        Long targetId = event.mediaId() != null ? event.mediaId() : event.episodeId();
        return targetId != null ? targetId.toString() : "unknown";
    }
}
