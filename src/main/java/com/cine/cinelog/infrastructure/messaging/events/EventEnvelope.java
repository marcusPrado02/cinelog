package com.cine.cinelog.infrastructure.messaging.events;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Envelope padronizado para todos os eventos Kafka.
 *
 * <p>
 * Garante consistência, versionamento e rastreabilidade de eventos
 * através de metadados obrigatórios e opcionais.
 * </p>
 *
 * <p>
 * <strong>Campos obrigatórios:</strong>
 * </p>
 * <ul>
 * <li>eventId: Identificador único do evento</li>
 * <li>type: Tipo do evento (ex: "WatchEntryCreated")</li>
 * <li>version: Versão do schema do payload</li>
 * <li>occurredAt: Timestamp de ocorrência</li>
 * <li>producer: Sistema produtor</li>
 * <li>payload: Dados do evento</li>
 * </ul>
 *
 * <p>
 * <strong>Metadados opcionais:</strong>
 * </p>
 * <ul>
 * <li>correlationId: ID para rastrear fluxo de requisições</li>
 * <li>causationId: ID do evento que causou este</li>
 * <li>traceparent: W3C Trace Context para rastreamento distribuído</li>
 * <li>userId: ID do usuário que originou a ação (se aplicável)</li>
 * </ul>
 *
 * @param <T> Tipo do payload do evento
 * @see <a href="https://www.w3.org/TR/trace-context/">W3C Trace Context</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> {

    /**
     * Identificador único do evento (UUID v4).
     * Usado para deduplicação e rastreamento.
     */
    @JsonProperty("event_id")
    private UUID eventId;

    /**
     * Tipo do evento em formato snake_case.
     * Exemplos: "watch_entry_created", "media_updated"
     */
    @JsonProperty("type")
    private String type;

    /**
     * Versão do schema do payload.
     * Segue versionamento semântico simplificado (inteiro incremental).
     * Versão 1 = schema inicial, versão 2 = mudanças compatíveis, etc.
     */
    @JsonProperty("version")
    private int version;

    /**
     * Timestamp de quando o evento ocorreu (UTC).
     * Gerado pelo produtor no momento da criação do evento.
     */
    @JsonProperty("occurred_at")
    private Instant occurredAt;

    /**
     * Nome do sistema produtor.
     * Padrão: "cinelog"
     */
    @JsonProperty("producer")
    private String producer;

    /**
     * Metadados adicionais para rastreamento e contexto.
     *
     * <p>
     * Campos comuns:
     * </p>
     * <ul>
     * <li>correlationId: UUID do fluxo de requisição</li>
     * <li>causationId: UUID do evento causador</li>
     * <li>traceparent: Contexto de trace distribuído (W3C)</li>
     * <li>userId: ID do usuário (se aplicável)</li>
     * </ul>
     */
    @JsonProperty("metadata")
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Payload do evento (schema específico por tipo).
     */
    @JsonProperty("payload")
    private T payload;

    /**
     * Valida se o envelope possui todos os campos obrigatórios.
     *
     * @return true se válido, false caso contrário
     */
    public boolean isValid() {
        return eventId != null
                && type != null && !type.isBlank()
                && version > 0
                && occurredAt != null
                && producer != null && !producer.isBlank()
                && payload != null;
    }

    /**
     * Adiciona metadado ao envelope.
     *
     * @param key   chave do metadado
     * @param value valor do metadado
     * @return este envelope (fluent interface)
     */
    public EventEnvelope<T> addMetadata(String key, String value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * Obtém metadado por chave.
     *
     * @param key chave do metadado
     * @return valor ou null se não existir
     */
    public String getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    /**
     * Obtém correlationId dos metadados.
     *
     * @return correlationId ou null
     */
    public String getCorrelationId() {
        return getMetadata("correlationId");
    }

    /**
     * Obtém causationId dos metadados.
     *
     * @return causationId ou null
     */
    public String getCausationId() {
        return getMetadata("causationId");
    }

    /**
     * Obtém traceparent dos metadados.
     *
     * @return traceparent ou null
     */
    public String getTraceparent() {
        return getMetadata("traceparent");
    }

    /**
     * Cria um novo envelope a partir deste, substituindo apenas o payload.
     * Útil para transformações de eventos.
     *
     * @param newPayload novo payload
     * @param <U>        tipo do novo payload
     * @return novo envelope
     */
    public <U> EventEnvelope<U> withPayload(U newPayload) {
        EventEnvelope<U> newEnvelope = new EventEnvelope<>();
        newEnvelope.setEventId(this.eventId);
        newEnvelope.setType(this.type);
        newEnvelope.setVersion(this.version);
        newEnvelope.setOccurredAt(this.occurredAt);
        newEnvelope.setProducer(this.producer);
        newEnvelope.setMetadata(new HashMap<>(this.metadata));
        newEnvelope.setPayload(newPayload);
        return newEnvelope;
    }

    @Override
    public String toString() {
        return String.format("EventEnvelope{eventId=%s, type='%s', version=%d, occurredAt=%s, producer='%s'}",
                eventId, type, version, occurredAt, producer);
    }
}
