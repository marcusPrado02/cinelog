package com.cine.cinelog.core.application.ports.events;

import com.cine.cinelog.core.domain.events.DomainEvent;

/**
 * Handler para processar Domain Events.
 *
 * <p>
 * Implementações devem ser stateless e idempotentes sempre que possível.
 * </p>
 *
 * @param <E> Tipo do evento a ser tratado
 * @since 1.1.0
 */
@FunctionalInterface
public interface DomainEventHandler<E extends DomainEvent> {

    /**
     * Processa o evento recebido.
     *
     * @param event Evento a ser processado
     */
    void handle(E event);

    /**
     * Tipo de evento que este handler processa.
     * Usado para roteamento de eventos.
     *
     * @return Class do evento
     */
    default Class<E> eventType() {
        // Reflection-based default - pode ser sobrescrito para performance
        @SuppressWarnings("unchecked")
        Class<E> type = (Class<E>) DomainEvent.class;
        return type;
    }
}
