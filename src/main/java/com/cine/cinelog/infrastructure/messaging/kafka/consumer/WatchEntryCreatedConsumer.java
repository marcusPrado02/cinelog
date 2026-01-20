package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import com.cine.cinelog.infrastructure.messaging.events.EventEnvelope;
import com.cine.cinelog.infrastructure.messaging.events.EventEnvelopeValidator;
import com.cine.cinelog.infrastructure.messaging.kafka.KafkaTopics;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventEntity;
import com.cine.cinelog.infrastructure.persistence.inbox.InboxEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consumer Kafka para eventos de criação de WatchEntry.
 *
 * <p>
 * <strong>Pattern Idempotent Consumer:</strong>
 * </p>
 * <ol>
 * <li>Recebe evento do Kafka (EventEnvelope JSON)</li>
 * <li>Valida envelope (eventId, type, version, payload não-nulos)</li>
 * <li>Verifica se eventId já existe na tabela inbox_event</li>
 * <li>Se já existe → SKIP (duplicate delivery, já processado)</li>
 * <li>Se não existe → processa lógica de negócio + insere na inbox (transação
 * atômica)</li>
 * <li>Commit manual do offset Kafka (ack.acknowledge())</li>
 * </ol>
 *
 * <p>
 * <strong>EventEnvelope Validation (v1.2.0):</strong>
 * </p>
 * <ul>
 * <li>Envelope inválido → enviar para DLQ (Dead Letter Queue)</li>
 * <li>Versão não suportada → enviar para DLQ</li>
 * <li>Payload inválido → enviar para DLQ</li>
 * </ul>
 *
 * <p>
 * <strong>Exactly-Once Semantics:</strong>
 * </p>
 * <ul>
 * <li>Kafka garante at-least-once delivery</li>
 * <li>Inbox pattern garante exactly-once processing</li>
 * <li>Transação SQL: processamento + insert inbox são atômicos</li>
 * </ul>
 *
 * <p>
 * <strong>Tracing & Correlation (PR4):</strong>
 * </p>
 * <ul>
 * <li>Extends BaseKafkaConsumer for automatic MDC setup</li>
 * <li>Extracts correlationId from Kafka headers</li>
 * <li>All logs include correlationId for distributed tracing</li>
 * </ul>
 *
 * <p>
 * <strong>Casos de Uso Exemplo:</strong>
 * </p>
 * <ul>
 * <li>Atualizar cache Redis quando WatchEntry é criado</li>
 * <li>Enviar notificação push para o usuário</li>
 * <li>Indexar no Elasticsearch para busca</li>
 * <li>Atualizar estatísticas agregadas (contador de filmes assistidos)</li>
 * </ul>
 */
@Component
public class WatchEntryCreatedConsumer extends BaseKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(WatchEntryCreatedConsumer.class);
    private static final String CONSUMER_NAME = "WatchEntryCreatedConsumer";
    private static final int SUPPORTED_VERSION = 1; // Versão suportada do evento

    private final InboxEventRepository inboxRepository;
    private final ObjectMapper objectMapper;
    private final EventEnvelopeValidator envelopeValidator;

    public WatchEntryCreatedConsumer(
            InboxEventRepository inboxRepository,
            ObjectMapper objectMapper,
            EventEnvelopeValidator envelopeValidator) {
        this.inboxRepository = inboxRepository;
        this.objectMapper = objectMapper;
        this.envelopeValidator = envelopeValidator;
    }

    /**
     * Consome eventos de criação de WatchEntry com idempotência e validação de
     * envelope.
     * <p>
     * PR4: Added correlation/tracing support via BaseKafkaConsumer
     *
     * @param record         ConsumerRecord com payload e headers
     * @param acknowledgment Callback para commit manual do offset
     */
    @KafkaListener(topics = KafkaTopics.WATCH_ENTRY_CREATED_V1, groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consume(
            ConsumerRecord<String, String> record,
            Acknowledgment acknowledgment) {

        // PR4: Setup MDC with correlationId from Kafka headers
        setupMDC(record);

        String envelopeJson = null;
        try {
            envelopeJson = record.value();
            String key = record.key();

            // 1. Deserializar e validar EventEnvelope
            EventEnvelope<?> envelope = deserializeAndValidateEnvelope(envelopeJson);

            UUID eventId = envelope.getEventId();
            log.info("Recebido EventEnvelope WatchEntryCreated: eventId={}, type={}, version={}, key={}",
                    eventId, envelope.getType(), envelope.getVersion(), key);

            // 2. Validar versão suportada
            if (envelope.getVersion() != SUPPORTED_VERSION) {
                log.error("Versão de evento não suportada: version={}, suportada={}, eventId={}",
                        envelope.getVersion(), SUPPORTED_VERSION, eventId);
                sendToDLQ(envelopeJson, "Unsupported event version: " + envelope.getVersion());
                acknowledgment.acknowledge();
                return;
            }

            // 3. Verificar idempotência (se já processou este evento)
            if (inboxRepository.existsByEventId(eventId)) {
                log.warn("Evento duplicado ignorado (já processado): eventId={}", eventId);
                acknowledgment.acknowledge(); // Commit offset mesmo para duplicatas
                return;
            }

            // 4. Extrair payload do envelope
            JsonNode payload = objectMapper.valueToTree(envelope.getPayload());

            // 5. Processar lógica de negócio
            processBusinessLogic(payload);

            // 6. Inserir na inbox (marca como processado)
            InboxEventEntity inboxEvent = new InboxEventEntity(
                    eventId,
                    CONSUMER_NAME,
                    envelope.getType(),
                    extractAggregateId(payload),
                    envelope.getOccurredAt(),
                    envelopeJson); // Armazena envelope completo para auditoria
            inboxEvent.markAsProcessed();
            inboxRepository.save(inboxEvent);

            // 7. Commit manual do offset Kafka
            acknowledgment.acknowledge();

            log.info("EventEnvelope processado com sucesso: eventId={}, correlationId={}",
                    eventId, envelope.getCorrelationId());

        } catch (InvalidEnvelopeException ex) {
            log.error("Envelope inválido recebido: {}", ex.getMessage(), ex);
            sendToDLQ(envelopeJson, "Invalid envelope: " + ex.getMessage());
            acknowledgment.acknowledge(); // Commit offset para evitar retry infinito
        } catch (Exception ex) {
            log.error("Erro ao processar evento WatchEntryCreated: {}", ex.getMessage(), ex);
            // Não faz acknowledge → Kafka fará retry (DefaultErrorHandler com backoff)
            throw new RuntimeException("Falha ao processar evento", ex);
        } finally {
            // PR4: Cleanup MDC to prevent memory leaks
            cleanupMDC();
        }
    }

    /**
     * Deserializa e valida EventEnvelope.
     *
     * @param envelopeJson JSON do envelope
     * @return EventEnvelope validado
     * @throws InvalidEnvelopeException se envelope for inválido
     */
    private EventEnvelope<?> deserializeAndValidateEnvelope(String envelopeJson) throws InvalidEnvelopeException {
        try {
            EventEnvelope<?> envelope = objectMapper.readValue(
                    envelopeJson,
                    objectMapper.getTypeFactory().constructParametricType(EventEnvelope.class, Object.class));

            // Validar envelope usando EventEnvelopeValidator
            EventEnvelopeValidator.ValidationResult validationResult = envelopeValidator.validate(envelope);
            if (!validationResult.isValid()) {
                throw new InvalidEnvelopeException("Envelope validation failed: " + validationResult.getErrorMessage());
            }

            return envelope;

        } catch (Exception ex) {
            throw new InvalidEnvelopeException("Failed to deserialize envelope: " + ex.getMessage(), ex);
        }
    }

    /**
     * Envia evento inválido para Dead Letter Queue.
     *
     * @param envelopeJson JSON do envelope inválido
     * @param reason       Motivo do envio
     */
    private void sendToDLQ(String envelopeJson, String reason) {
        log.error("Enviando evento para DLQ: reason={}, payload={}", reason,
                envelopeJson.substring(0, Math.min(500, envelopeJson.length())));
        // TODO PR3: Implementar envio para DLQ (cinelog.dlq)
        // - Persistir em tabela dlq_event
        // - Publicar no tópico cinelog.dlq
        // - Incluir metadata: reason, consumer, timestamp, originalTopic
    }

    /**
     * Lógica de negócio a ser executada quando um WatchEntry é criado.
     *
     * <p>
     * <strong>Exemplos de implementação futura:</strong>
     * </p>
     * <ul>
     * <li>Invalidar cache Redis de estatísticas do usuário</li>
     * <li>Enviar notificação push/email</li>
     * <li>Indexar no Elasticsearch</li>
     * <li>Atualizar contador agregado de filmes assistidos</li>
     * </ul>
     */
    private void processBusinessLogic(JsonNode payload) {
        try {
            Long watchEntryId = payload.path("watchEntryId").asLong();
            Long userId = payload.path("userId").asLong();
            Long mediaId = payload.path("mediaId").asLong();

            log.info("Processando WatchEntry criado: watchEntryId={}, userId={}, mediaId={}",
                    watchEntryId, userId, mediaId);

            // TODO: Implementar lógica de negócio real
            // Exemplos:
            // - cacheService.invalidateUserStats(userId);
            // - notificationService.sendPushNotification(userId, "Filme adicionado!");
            // - elasticsearchService.indexWatchEntry(watchEntryId);
            // - statisticsService.incrementWatchCount(userId);

        } catch (Exception ex) {
            log.error("Erro ao processar lógica de negócio: {}", ex.getMessage(), ex);
            throw new RuntimeException("Falha no processamento", ex);
        }
    }

    /**
     * Extrai aggregateId (watchEntryId) do payload.
     */
    private String extractAggregateId(JsonNode payload) {
        try {
            return payload.path("watchEntryId").asText();
        } catch (Exception ex) {
            log.warn("Não foi possível extrair aggregateId do payload", ex);
            return "UNKNOWN";
        }
    }

    /**
     * Exception para envelope inválido.
     */
    private static class InvalidEnvelopeException extends Exception {
        public InvalidEnvelopeException(String message) {
            super(message);
        }

        public InvalidEnvelopeException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
