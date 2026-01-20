package com.cine.cinelog.core.application.ports.events;

import com.cine.cinelog.core.domain.events.DomainEvent;

/**
 * Port (interface) para publicação de Domain Events.
 *
 * <p>
 * Define contrato para publicação de eventos do domínio, seguindo Hexagonal
 * Architecture.
 * Implementações podem variar: dispatcher em memória, outbox transacional,
 * messaging direto, etc.
 * </p>
 *
 * <p>
 * Responsabilidades:
 * </p>
 * <ul>
 * <li>Publicar eventos de forma confiável</li>
 * <li>Garantir transactionalidade quando necessário (via Outbox Pattern)</li>
 * <li>Permitir desacoplamento entre domínio e infraestrutura de eventos</li>
 * </ul>
 *
 * @since 1.1.0
 */
public interface DomainEventPublisherPort {

    /**
     * Publica um Domain Event.
     *
     * <p>
     * Dependendo da implementação:
     * </p>
     * <ul>
     * <li>Dispatcher em memória: notifica handlers síncronos</li>
     * <li>Outbox: persiste evento para publicação posterior</li>
     * <li>Mensageria: publica diretamente em tópico/fila</li>
     * </ul>
     *
     * @param event Evento a ser publicado
     */
    void publish(DomainEvent event);

    /**
     * Publica múltiplos eventos em uma única operação.
     * Útil para batch operations ou sagas.
     *
     * @param events Eventos a serem publicados
     */
    default void publishAll(Iterable<DomainEvent> events) {
        events.forEach(this::publish);
    }
}
