package com.cine.cinelog.infrastructure.messaging.events;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Factory para criação padronizada de EventEnvelopes.
 *
 * <p>
 * Centraliza a lógica de criação de envelopes, garantindo:
 * </p>
 * <ul>
 * <li>Geração automática de eventId</li>
 * <li>Timestamp correto (UTC)</li>
 * <li>Producer fixo ("cinelog")</li>
 * <li>Extração de metadados do MDC (correlationId, traceparent)</li>
 * </ul>
 */
@Component
public class EventEnvelopeFactory {

    private static final String PRODUCER_NAME = "cinelog";

    /**
     * Cria um envelope de evento com metadados do contexto atual.
     *
     * @param type    tipo do evento (ex: "watch_entry_created")
     * @param version versão do schema
     * @param payload dados do evento
     * @param <T>     tipo do payload
     * @return envelope configurado
     */
    public <T> EventEnvelope<T> create(String type, int version, T payload) {
        return create(type, version, payload, extractMetadataFromContext());
    }

    /**
     * Cria um envelope de evento com metadados customizados.
     *
     * @param type           tipo do evento
     * @param version        versão do schema
     * @param payload        dados do evento
     * @param customMetadata metadados adicionais
     * @param <T>            tipo do payload
     * @return envelope configurado
     */
    public <T> EventEnvelope<T> create(String type, int version, T payload, Map<String, String> customMetadata) {
        Map<String, String> metadata = new HashMap<>(extractMetadataFromContext());
        if (customMetadata != null) {
            metadata.putAll(customMetadata);
        }

        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .type(type)
                .version(version)
                .occurredAt(Instant.now())
                .producer(PRODUCER_NAME)
                .metadata(metadata)
                .payload(payload)
                .build();
    }

    /**
     * Cria um envelope como resposta/consequência de outro evento.
     * Define causationId = eventId do evento original.
     *
     * @param type         tipo do novo evento
     * @param version      versão do schema
     * @param payload      dados do novo evento
     * @param causingEvent evento que causou este
     * @param <T>          tipo do novo payload
     * @param <U>          tipo do payload original
     * @return envelope configurado com causationId
     */
    public <T, U> EventEnvelope<T> createCausedBy(String type, int version, T payload, EventEnvelope<U> causingEvent) {
        Map<String, String> metadata = new HashMap<>();

        // Propaga correlationId
        String correlationId = causingEvent.getCorrelationId();
        if (correlationId != null) {
            metadata.put("correlationId", correlationId);
        }

        // Define causationId como eventId do evento original
        metadata.put("causationId", causingEvent.getEventId().toString());

        // Propaga traceparent se existir
        String traceparent = causingEvent.getTraceparent();
        if (traceparent != null) {
            metadata.put("traceparent", traceparent);
        }

        return create(type, version, payload, metadata);
    }

    /**
     * Extrai metadados do contexto de execução (MDC).
     *
     * <p>
     * Busca automaticamente:
     * </p>
     * <ul>
     * <li>correlationId: do MDC ou gera novo UUID</li>
     * <li>traceparent: do MDC se disponível</li>
     * <li>userId: do MDC se disponível (contexto de segurança)</li>
     * </ul>
     *
     * @return mapa de metadados
     */
    private Map<String, String> extractMetadataFromContext() {
        Map<String, String> metadata = new HashMap<>();

        // CorrelationId: tenta obter do MDC, senão gera novo
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        metadata.put("correlationId", correlationId);

        // Traceparent: W3C Trace Context
        String traceparent = MDC.get("traceparent");
        if (traceparent != null && !traceparent.isBlank()) {
            metadata.put("traceparent", traceparent);
        }

        // UserId: do contexto de segurança (se disponível)
        String userId = MDC.get("userId");
        if (userId != null && !userId.isBlank()) {
            metadata.put("userId", userId);
        }

        return metadata;
    }
}
