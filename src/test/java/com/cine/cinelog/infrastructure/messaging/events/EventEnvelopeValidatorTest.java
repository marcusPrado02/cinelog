package com.cine.cinelog.infrastructure.messaging.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes unitários para EventEnvelopeValidator.
 */
@DisplayName("EventEnvelopeValidator")
class EventEnvelopeValidatorTest {

    private final EventEnvelopeValidator validator = new EventEnvelopeValidator();

    @Test
    @DisplayName("Deve validar envelope válido com sucesso")
    void shouldValidateValidEnvelope() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("test-payload");

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar envelope nulo")
    void shouldRejectNullEnvelope() {
        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(null);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Envelope is null");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com eventId nulo")
    void shouldRejectNullEventId() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setEventId(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("eventId is null");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com type nulo")
    void shouldRejectNullType() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setType(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("type is null or blank");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com type vazio")
    void shouldRejectBlankType() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setType("   ");

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("type is null or blank");
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, -1, -99 })
    @DisplayName("Deve rejeitar envelope com version inválida")
    void shouldRejectInvalidVersion(int invalidVersion) {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setVersion(invalidVersion);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("version must be greater than 0");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com occurredAt nulo")
    void shouldRejectNullOccurredAt() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setOccurredAt(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("occurredAt is null");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com producer nulo")
    void shouldRejectNullProducer() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setProducer(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("producer is null or blank");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com producer vazio")
    void shouldRejectBlankProducer() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setProducer("");

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("producer is null or blank");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com payload nulo")
    void shouldRejectNullPayload() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setPayload(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("payload is null");
    }

    @Test
    @DisplayName("Deve rejeitar envelope com múltiplos erros")
    void shouldRejectEnvelopeWithMultipleErrors() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(null) // erro 1
                .type(null) // erro 2
                .version(0) // erro 3
                .occurredAt(null) // erro 4
                .producer(null) // erro 5
                .payload(null) // erro 6
                .build();

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage())
                .contains("eventId is null")
                .contains("type is null or blank")
                .contains("version must be greater than 0")
                .contains("occurredAt is null")
                .contains("producer is null or blank")
                .contains("payload is null");
    }

    @Test
    @DisplayName("Deve validar envelope com metadata vazio")
    void shouldValidateEnvelopeWithEmptyMetadata() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setMetadata(new HashMap<>());

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve validar envelope com metadata null")
    void shouldValidateEnvelopeWithNullMetadata() {
        // Arrange
        EventEnvelope<String> envelope = createValidEnvelope("payload");
        envelope.setMetadata(null);

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isTrue(); // metadata é opcional
    }

    @Test
    @DisplayName("Deve lançar exceção ao usar validateOrThrow com envelope inválido")
    void shouldThrowExceptionWhenUsingValidateOrThrow() {
        // Arrange
        EventEnvelope<String> invalidEnvelope = createValidEnvelope("payload");
        invalidEnvelope.setEventId(null);

        // Act & Assert
        assertThatThrownBy(() -> validator.validateOrThrow(invalidEnvelope))
                .isInstanceOf(EventEnvelopeValidator.InvalidEventEnvelopeException.class)
                .hasMessageContaining("Invalid event envelope")
                .hasMessageContaining("eventId is null");
    }

    @Test
    @DisplayName("Não deve lançar exceção ao usar validateOrThrow com envelope válido")
    void shouldNotThrowExceptionWhenUsingValidateOrThrowWithValidEnvelope() {
        // Arrange
        EventEnvelope<String> validEnvelope = createValidEnvelope("test-payload");

        // Act & Assert - não deve lançar exceção
        validator.validateOrThrow(validEnvelope);
    }

    @Test
    @DisplayName("Deve validar envelope com todos os metadados preenchidos")
    void shouldValidateEnvelopeWithAllMetadata() {
        // Arrange
        Map<String, String> metadata = new HashMap<>();
        metadata.put("correlationId", UUID.randomUUID().toString());
        metadata.put("causationId", UUID.randomUUID().toString());
        metadata.put("traceparent", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
        metadata.put("userId", "12345");

        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test.event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(metadata)
                .payload("test-payload")
                .build();

        // Act
        EventEnvelopeValidator.ValidationResult result = validator.validate(envelope);

        // Assert
        assertThat(result.isValid()).isTrue();
    }

    // Helper methods

    private <T> EventEnvelope<T> createValidEnvelope(T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID())
                .type("test.event.created")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload(payload)
                .build();
    }
}
