package com.cine.cinelog.infrastructure.events;

import com.cine.cinelog.core.application.ports.events.DomainEventHandler;
import com.cine.cinelog.core.application.ports.events.DomainEventPublisherPort;
import com.cine.cinelog.core.domain.events.DomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dispatcher interno de Domain Events em memória (síncrono).
 *
 * <p>
 * Implementação simples para processar eventos internos da aplicação antes da
 * introdução do Outbox Pattern.
 * Útil para:
 * </p>
 * <ul>
 * <li>Side-effects síncronos dentro da mesma transação</li>
 * <li>Validações cross-aggregate</li>
 * <li>Auditoria interna</li>
 * </ul>
 *
 * <p>
 * <strong>Limitações:</strong>
 * </p>
 * <ul>
 * <li>Eventos são processados síncronamente (aumenta latência)</li>
 * <li>Não garante entrega em caso de falha após commit</li>
 * <li>Não persiste eventos para replay</li>
 * </ul>
 *
 * <p>
 * Para eventos que precisam sair da aplicação ou garantia de entrega, use
 * OutboxEventPublisher.
 * </p>
 *
 * @since 1.1.0
 */
@Component
@Profile("!outbox") // Desabilitado quando Outbox está ativo
public class InMemoryDomainEventDispatcher implements DomainEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(InMemoryDomainEventDispatcher.class);

    private final Map<Class<? extends DomainEvent>, List<DomainEventHandler<? extends DomainEvent>>> handlers;

    public InMemoryDomainEventDispatcher(List<DomainEventHandler<? extends DomainEvent>> eventHandlers) {
        this.handlers = new ConcurrentHashMap<>();

        // Registra handlers automaticamente (Spring injeta todos os beans)
        eventHandlers.forEach(handler -> {
            Class<? extends DomainEvent> eventType = handler.eventType();
            handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
            log.info("Registered DomainEventHandler: {} for event type: {}",
                    handler.getClass().getSimpleName(), eventType.getSimpleName());
        });
    }

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing event: {} (ID: {})", event.type(), event.eventId());

        Class<? extends DomainEvent> eventClass = event.getClass();
        List<DomainEventHandler<? extends DomainEvent>> eventHandlers = handlers.get(eventClass);

        if (eventHandlers == null || eventHandlers.isEmpty()) {
            log.debug("No handlers registered for event type: {}", eventClass.getSimpleName());
            return;
        }

        for (DomainEventHandler<? extends DomainEvent> handler : eventHandlers) {
            try {
                log.debug("Dispatching event {} to handler {}", event.eventId(), handler.getClass().getSimpleName());
                @SuppressWarnings("unchecked")
                DomainEventHandler<DomainEvent> typedHandler = (DomainEventHandler<DomainEvent>) handler;
                typedHandler.handle(event);
            } catch (Exception e) {
                log.error("Error handling event {} with handler {}: {}",
                        event.eventId(), handler.getClass().getSimpleName(), e.getMessage(), e);
                // Não propaga exceção para não quebrar transação principal
                // Em produção, considere publicar em DLQ ou retry mechanism
            }
        }
    }

    /**
     * Registra handler dinamicamente (se necessário).
     */
    public <E extends DomainEvent> void registerHandler(Class<E> eventType, DomainEventHandler<E> handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
        log.info("Dynamically registered handler: {} for event: {}",
                handler.getClass().getSimpleName(), eventType.getSimpleName());
    }
}
