package com.cine.cinelog.infrastructure.messaging.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validador de EventEnvelope para garantir integridade dos eventos Kafka.
 *
 * <p>
 * Valida campos obrigatórios do envelope conforme especificação:
 * </p>
 * <ul>
 * <li>eventId: não nulo</li>
 * <li>type: não nulo e não vazio</li>
 * <li>version: maior que 0</li>
 * <li>occurredAt: não nulo</li>
 * <li>producer: não nulo e não vazio</li>
 * <li>payload: não nulo</li>
 * </ul>
 *
 * <p>
 * Eventos inválidos devem ser rejeitados e enviados para Dead Letter Queue
 * (DLQ).
 * </p>
 *
 * @see EventEnvelope
 */
@Slf4j
@Component
public class EventEnvelopeValidator {

    /**
     * Valida um envelope de evento.
     *
     * @param envelope envelope a validar
     * @param <T>      tipo do payload
     * @return resultado da validação
     */
    public <T> ValidationResult validate(EventEnvelope<T> envelope) {
        if (envelope == null) {
            return ValidationResult.invalid("Envelope is null");
        }

        List<String> errors = new ArrayList<>();

        if (envelope.getEventId() == null) {
            errors.add("eventId is null");
        }

        if (envelope.getType() == null || envelope.getType().isBlank()) {
            errors.add("type is null or blank");
        }

        if (envelope.getVersion() <= 0) {
            errors.add("version must be greater than 0 (actual: " + envelope.getVersion() + ")");
        }

        if (envelope.getOccurredAt() == null) {
            errors.add("occurredAt is null");
        }

        if (envelope.getProducer() == null || envelope.getProducer().isBlank()) {
            errors.add("producer is null or blank");
        }

        if (envelope.getPayload() == null) {
            errors.add("payload is null");
        }

        if (errors.isEmpty()) {
            log.debug("Envelope validation succeeded: {}", envelope);
            return ValidationResult.valid();
        } else {
            log.warn("Envelope validation failed: {} - errors: {}", envelope, errors);
            return ValidationResult.invalid(String.join(", ", errors));
        }
    }

    /**
     * Valida um envelope e lança exceção se inválido.
     *
     * @param envelope envelope a validar
     * @param <T>      tipo do payload
     * @throws InvalidEventEnvelopeException se envelope inválido
     */
    public <T> void validateOrThrow(EventEnvelope<T> envelope) {
        ValidationResult result = validate(envelope);
        if (!result.isValid()) {
            throw new InvalidEventEnvelopeException(result.getErrorMessage());
        }
    }

    /**
     * Resultado da validação.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Exceção lançada quando envelope é inválido.
     */
    public static class InvalidEventEnvelopeException extends RuntimeException {
        public InvalidEventEnvelopeException(String message) {
            super("Invalid event envelope: " + message);
        }
    }
}
