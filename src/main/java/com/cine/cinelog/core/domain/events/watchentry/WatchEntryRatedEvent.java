package com.cine.cinelog.core.domain.events.watchentry;

import com.cine.cinelog.core.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento emitido quando um WatchEntry recebe uma avaliação (rating).
 * Pode ser na criação ou em uma atualização posterior.
 *
 * @param eventId        Identificador único do evento
 * @param occurredAt     Timestamp de quando o evento ocorreu
 * @param watchEntryId   ID do WatchEntry avaliado
 * @param userId         ID do usuário que avaliou
 * @param mediaId        ID da mídia avaliada (pode ser null se for episódio)
 * @param episodeId      ID do episódio avaliado (pode ser null se for mídia
 *                       direta)
 * @param rating         Nota dada (0 a 10)
 * @param previousRating Nota anterior (null se primeira avaliação)
 *
 * @since 1.1.0
 */
public record WatchEntryRatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long watchEntryId,
        Long userId,
        Long mediaId,
        Long episodeId,
        BigDecimal rating,
        BigDecimal previousRating) implements DomainEvent {

    public static final String EVENT_TYPE = "watchentry.rated";
    public static final int EVENT_VERSION = 1;

    /**
     * Factory method para criar evento.
     */
    public static WatchEntryRatedEvent of(
            Long watchEntryId,
            Long userId,
            Long mediaId,
            Long episodeId,
            BigDecimal rating,
            BigDecimal previousRating) {
        return new WatchEntryRatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                watchEntryId,
                userId,
                mediaId,
                episodeId,
                rating,
                previousRating);
    }

    @Override
    public String type() {
        return EVENT_TYPE;
    }

    @Override
    public int version() {
        return EVENT_VERSION;
    }

    @Override
    public String aggregateId() {
        return watchEntryId != null ? watchEntryId.toString() : null;
    }

    @Override
    public String aggregateType() {
        return "WatchEntry";
    }
}
