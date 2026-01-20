package com.cine.cinelog.infrastructure.events.handlers;

import com.cine.cinelog.core.application.ports.events.DomainEventHandler;
import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Handler de exemplo para processar eventos de criação de WatchEntry.
 *
 * <p>
 * Este handler demonstra o padrão Observer/Mediator para Domain Events.
 * Em produção, pode ser usado para:
 * </p>
 * <ul>
 * <li>Atualizar estatísticas agregadas (contadores, médias)</li>
 * <li>Enviar notificações para usuários</li>
 * <li>Auditoria e logging avançado</li>
 * <li>Invalidar caches relacionados</li>
 * <li>Trigger de jobs assíncronos</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Component
public class WatchEntryCreatedEventHandler implements DomainEventHandler<WatchEntryCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(WatchEntryCreatedEventHandler.class);

    @Override
    public void handle(WatchEntryCreatedEvent event) {
        log.info("Processing WatchEntryCreatedEvent: eventId={}, watchEntryId={}, userId={}, mediaId={}, episodeId={}",
                event.eventId(),
                event.watchEntryId(),
                event.userId(),
                event.mediaId(),
                event.episodeId());

        // Exemplo: atualizar estatísticas do usuário (poderia chamar um usecase)
        if (event.rating() != null) {
            log.debug("User {} rated media/episode with {}", event.userId(), event.rating());
            // Poderia chamar UpdateUserStatsUseCase ou similar
        }

        // Exemplo: invalidar cache de recomendações
        log.debug("Invalidating recommendation cache for user {}", event.userId());

        // Em produção: poderia enviar evento para sistema de notificações, etc.
    }

    @Override
    public Class<WatchEntryCreatedEvent> eventType() {
        return WatchEntryCreatedEvent.class;
    }
}
