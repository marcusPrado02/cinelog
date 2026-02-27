package com.cine.cinelog.shared.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * A08:2025 — Verificador de integridade para eventos Kafka.
 *
 * <p>
 * Verifica que payloads de eventos Kafka não foram adulterados em trânsito
 * usando HMAC-SHA256. O produtor assina o payload e coloca o HMAC no header
 * {@code X-Integrity-HMAC}. O consumidor verifica antes de processar.
 * </p>
 *
 * <h3>Cenário de ataque:</h3>
 * 
 * <pre>
 * Produtor → [payload + HMAC_header] → Kafka Broker → Consumidor
 *                                          ↑
 *                               Atacante com acesso ao broker
 *                               modifica o payload JSON
 *                                          ↓
 *                               HMAC não bate → REJEITADO
 * </pre>
 *
 * @since 1.3.0
 */
@Component
@Slf4j
public class KafkaEventIntegrityVerifier {

    public static final String INTEGRITY_HEADER = "X-Integrity-HMAC";

    private final IntegrityService integrityService;

    public KafkaEventIntegrityVerifier(IntegrityService integrityService) {
        this.integrityService = integrityService;
    }

    /**
     * Assina um payload para inclusão no header do Kafka record.
     *
     * @param payload JSON serializado do evento
     * @return HMAC-SHA256 em Base64
     */
    public String signPayload(String payload) {
        return integrityService.sign(payload);
    }

    /**
     * Verifica a integridade de um ConsumerRecord do Kafka.
     *
     * @param record registro recebido do Kafka
     * @return true se o HMAC é válido, ou se não há header (backward compatible)
     */
    public boolean verifyRecord(ConsumerRecord<String, ?> record) {
        Header hmacHeader = record.headers().lastHeader(INTEGRITY_HEADER);

        if (hmacHeader == null) {
            // Backward compatibility: eventos sem HMAC são aceitos com warning
            log.warn("A08:2025 — Evento Kafka sem header de integridade: topic={}, partition={}, offset={}",
                    record.topic(), record.partition(), record.offset());
            return true;
        }

        String expectedHmac = new String(hmacHeader.value(), StandardCharsets.UTF_8);
        String payload = record.value() != null ? record.value().toString() : "";

        boolean valid = integrityService.verify(payload, expectedHmac);

        if (!valid) {
            log.error("A08:2025 — INTEGRIDADE VIOLADA em evento Kafka: topic={}, partition={}, offset={}, key={}",
                    record.topic(), record.partition(), record.offset(), record.key());
        }

        return valid;
    }

    /**
     * Gera bytes do HMAC para uso como header value no ProducerRecord.
     */
    public byte[] signPayloadAsBytes(String payload) {
        return signPayload(payload).getBytes(StandardCharsets.UTF_8);
    }
}
