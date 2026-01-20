package com.cine.cinelog.infrastructure.events;

import com.cine.cinelog.core.application.ports.events.DomainEventPublisherPort;
import com.cine.cinelog.core.domain.events.DomainEvent;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventMapper;
import com.cine.cinelog.infrastructure.persistence.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adapter que grava Domain Events na tabela outbox transacionalmente.
 *
 * <p>
 * Implementa o Outbox Pattern:
 * </p>
 * <ol>
 * <li>DomainEvent é convertido para OutboxEventEntity</li>
 * <li>Gravado na mesma transação do agregado</li>
 * <li>OutboxPublisherJob processa eventos PENDING posteriormente</li>
 * </ol>
 *
 * <p>
 * Benefícios:
 * </p>
 * <ul>
 * <li>Consistência transacional garantida (agregado + evento)</li>
 * <li>At-least-once delivery (com retry automático)</li>
 * <li>Desacoplamento da mensageria (Kafka pode estar offline)</li>
 * <li>Auditoria completa de eventos</li>
 * </ul>
 *
 * @since 1.1.0
 */
@Component
@Primary
@Profile("outbox") // Ativa apenas quando perfil 'outbox' estiver ativo
public class OutboxEventWriterAdapter implements DomainEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventWriterAdapter.class);

    private final OutboxEventRepository repository;
    private final OutboxEventMapper mapper;

    public OutboxEventWriterAdapter(
            OutboxEventRepository repository,
            OutboxEventMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Grava evento no outbox dentro da transação corrente.
     *
     * <p>
     * Importante: Este método deve ser chamado dentro de uma transação ativa
     * (normalmente a mesma transação que persiste o agregado).
     * </p>
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(DomainEvent event) {
        log.debug("Writing DomainEvent to outbox: eventType={}, eventId={}, aggregateId={}",
                event.type(), event.eventId(), event.aggregateId());

        OutboxEventEntity entity = mapper.toEntity(event);
        repository.save(entity);

        log.info("DomainEvent written to outbox successfully: eventId={}, eventType={}",
                event.eventId(), event.type());
    }
}
