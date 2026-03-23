package com.cine.cinelog.infrastructure.messaging.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Configuração de Kafka Consumer com estratégia de retry e Dead Letter Queue
 * (DLQ).
 *
 * <p>
 * <strong>Estratégia de Error Handling:</strong>
 * </p>
 * <ul>
 * <li>Retries com exponential backoff (1s, 2s, 4s)</li>
 * <li>Após 3 tentativas sem sucesso, publica na DLQ (cinelog.dlq)</li>
 * <li>DLQ preserva headers originais + exception stacktrace</li>
 * </ul>
 *
 * <p>
 * <strong>Idempotência:</strong>
 * </p>
 * <ul>
 * <li>Consumer verifica inbox_event antes de processar</li>
 * <li>Se eventId já existe → skip (duplicate)</li>
 * <li>Se não existe → processa + insere inbox (transacional)</li>
 * <li>Commit manual do offset após sucesso</li>
 * </ul>
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    /**
     * Configura error handler com retry + DLQ.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // Dead Letter Publishing Recoverer - envia para DLQ após falhas
        var deadLetterPublisher = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> {
                    // Define tópico DLQ fixo
                    String dlqTopic = "cinelog.dlq";
                    log.error("Enviando mensagem para DLQ: topic={}, key={}, exception={}",
                            dlqTopic, record.key(), ex.getMessage());
                    return new org.apache.kafka.common.TopicPartition(dlqTopic, 0);
                });

        // Exponential backoff: 1s, 2s, 4s (3 retries)
        var backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(10_000L);

        var errorHandler = new DefaultErrorHandler(deadLetterPublisher, backOff);

        // Log cada retry
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            if (record instanceof ConsumerRecord<?, ?> consumerRecord) {
                log.warn("Retry #{} para mensagem: topic={}, partition={}, offset={}, exception={}",
                        deliveryAttempt,
                        consumerRecord.topic(),
                        consumerRecord.partition(),
                        consumerRecord.offset(),
                        ex.getMessage());
            }
        });

        return errorHandler;
    }

    /**
     * Factory customizada para listener containers (pode ser expandida no futuro).
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            CommonErrorHandler kafkaErrorHandler,
            @Value("${spring.kafka.listener.auto-startup:true}") boolean autoStartup) {

        var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        factory.setAutoStartup(autoStartup);

        return factory;
    }
}
