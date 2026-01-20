package com.cine.cinelog.core.domain.events.watchentry;

import com.cine.cinelog.core.domain.events.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Evento emitido quando um WatchEntry é criado.
 * Representa o registro de uma nova mídia assistida/em andamento por um
 * usuário.
 *
 * @param eventId      Identificador único do evento
 * @param occurredAt   Timestamp de quando o evento ocorreu
 * @param watchEntryId ID do WatchEntry criado
 * @param userId       ID do usuário que criou
 * @param mediaId      ID da mídia assistida (pode ser null se for episódio)
 * @param episodeId    ID do episódio assistido (pode ser null se for mídia
 *                     direta)
 * @param watchedAt    Data que foi assistida
 * @param rating       Nota dada (se houver)
 *
 * @since 1.1.0
 */
public record WatchEntryCreatedEvent(
        UUID eventId,
        Instant occurredAt,
        Long watchEntryId,
        Long userId,
        Long mediaId,
        Long episodeId,
        LocalDate watchedAt,
        BigDecimal rating) implements DomainEvent {

    public static final String EVENT_TYPE = "watchentry.created";
    public static final int EVENT_VERSION = 1;

    /**
     * Factory method para criar evento a partir de dados do domínio.
     */
    public static WatchEntryCreatedEvent of(
            Long watchEntryId,
            Long userId,
            Long mediaId,
            Long episodeId,
            LocalDate watchedAt,
            BigDecimal rating) {
        return new WatchEntryCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                watchEntryId,
                userId,
                mediaId,
                episodeId,
                watchedAt,
                rating);
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
