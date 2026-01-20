package com.cine.cinelog.core.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Interface base para todos os Domain Events no sistema.
 *
 * <p>
 * Domain Events são mensagens que representam algo significativo que aconteceu
 * no domínio.
 * Seguem padrão de eventos imutáveis e servem como base para event sourcing,
 * CQRS e integrações assíncronas.
 * </p>
 *
 * <p>
 * Todos os eventos devem:
 * </p>
 * <ul>
 * <li>Ser imutáveis (record ou class final com campos final)</li>
 * <li>Conter apenas dados primitivos, strings, UUIDs ou value objects</li>
 * <li>Ter um eventId único para rastreamento e idempotência</li>
 * <li>Registrar quando o evento ocorreu (occurredAt)</li>
 * <li>Ter um tipo identificador (type) para roteamento</li>
 * <li>Ter versionamento para evolução do schema</li>
 * </ul>
 *
 * @since 1.1.0
 */
public interface DomainEvent {

    /**
     * Identificador único do evento.
     * Usado para idempotência e rastreamento distribuído.
     *
     * @return UUID único do evento
     */
    UUID eventId();

    /**
     * Timestamp de quando o evento ocorreu no domínio.
     *
     * @return Instant da ocorrência do evento
     */
    Instant occurredAt();

    /**
     * Tipo do evento, usado para roteamento e desserialização.
     * Deve seguir convenção: aggregate.action (ex: watchentry.created, media.rated)
     *
     * @return String representando o tipo do evento
     */
    String type();

    /**
     * Versão do schema do evento para controle de evolução.
     * Permite versionamento de eventos e backward compatibility.
     *
     * @return Versão inteira do schema (default 1)
     */
    default int version() {
        return 1;
    }

    /**
     * ID do agregado que gerou o evento (opcional, mas recomendado).
     *
     * @return ID do agregado ou null se não aplicável
     */
    default String aggregateId() {
        return null;
    }

    /**
     * Tipo do agregado que gerou o evento (opcional, mas recomendado).
     *
     * @return Tipo do agregado (ex: "WatchEntry", "Media") ou null
     */
    default String aggregateType() {
        return null;
    }
}
