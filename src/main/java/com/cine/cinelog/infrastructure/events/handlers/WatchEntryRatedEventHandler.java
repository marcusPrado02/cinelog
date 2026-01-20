package com.cine.cinelog.infrastructure.events.handlers;

import com.cine.cinelog.core.application.ports.events.DomainEventHandler;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryRatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler para processar eventos de avaliação (rating) de WatchEntry.
 *
 * <p>
 * Demonstra processamento de eventos específicos de rating, útil para:
 * </p>
 * <ul>
 * <li>Recalcular médias de avaliação de mídia</li>
 * <li>Atualizar rankings e recomendações</li>
 * <li>Análise de comportamento do usuário</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Component
public class WatchEntryRatedEventHandler implements DomainEventHandler<WatchEntryRatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(WatchEntryRatedEventHandler.class);

    @Override
    public void handle(WatchEntryRatedEvent event) {
        log.info(
                "Processing WatchEntryRatedEvent: eventId={}, watchEntryId={}, userId={}, rating={}, previousRating={}",
                event.eventId(),
                event.watchEntryId(),
                event.userId(),
                event.rating(),
                event.previousRating());

        // Exemplo: recalcular média da mídia
        Long mediaId = event.mediaId() != null ? event.mediaId() : event.episodeId();
        if (mediaId != null) {
            log.debug("Recalculating average rating for media/episode ID: {}", mediaId);
            // Poderia chamar RecalculateMediaRatingUseCase
        }

        // Exemplo: atualizar sistema de recomendações
        if (event.rating() != null && event.rating().doubleValue() >= 8.0) {
            log.debug("User {} highly rated content, updating recommendations", event.userId());
        }
    }

    @Override
    public Class<WatchEntryRatedEvent> eventType() {
        return WatchEntryRatedEvent.class;
    }
}
