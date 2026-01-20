package com.cine.cinelog.infrastructure.messaging.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para EventEnvelope.
 *
 * <p>
 * Valida:
 * <ul>
 * <li>Validação de campos obrigatórios (isValid())</li>
 * <li>Fluent API para metadata</li>
 * <li>Métodos de extração de metadata (correlationId, causationId,
 * traceparent)</li>
 * <li>Transformação de payload (withPayload())</li>
 * </ul>
 * </p>
 */
@DisplayName("EventEnvelope - Generic Event Wrapper")
class EventEnvelopeTest {

    @Test
    @DisplayName("Deve validar envelope com todos os campos obrigatórios presentes")
    void testIsValid_AllFieldsPresent_ReturnsTrue() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando eventId é null")
    void testIsValid_MissingEventId_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(null) // Missing
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando type é null")
    void testIsValid_NullType_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type(null) // Null
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando type é vazio (blank)")
    void testIsValid_BlankType_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("   ") // Blank
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando version é zero")
    void testIsValid_VersionZero_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(0) // Invalid
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando version é negativo")
    void testIsValid_NegativeVersion_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(-1) // Invalid
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando occurredAt é null")
    void testIsValid_NullOccurredAt_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(null) // Null
                .producer("cinelog")
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando producer é null")
    void testIsValid_NullProducer_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer(null) // Null
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando producer é vazio (blank)")
    void testIsValid_BlankProducer_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("  ") // Blank
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve invalidar envelope quando payload é null")
    void testIsValid_NullPayload_ReturnsFalse() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .payload(null) // Null
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Deve adicionar metadata usando fluent API")
    void testAddMetadata_FluentAPI() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("test payload")
                .build();

        // Act
        EventEnvelope<String> updated = envelope
                .addMetadata("key1", "value1")
                .addMetadata("key2", "value2");

        // Assert
        assertThat(updated.getMetadata()).containsEntry("key1", "value1");
        assertThat(updated.getMetadata()).containsEntry("key2", "value2");
        assertThat(updated).isSameAs(envelope); // Fluent API retorna this
    }

    @Test
    @DisplayName("Deve extrair correlationId do metadata")
    void testGetCorrelationId_ReturnsFromMetadata() {
        // Arrange
        String correlationId = "corr-123-456";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("correlationId", correlationId);

        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(metadata)
                .payload("test payload")
                .build();

        // Act
        String extracted = envelope.getCorrelationId();

        // Assert
        assertThat(extracted).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve retornar null quando correlationId não existe no metadata")
    void testGetCorrelationId_NotPresent_ReturnsNull() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("test payload")
                .build();

        // Act
        String extracted = envelope.getCorrelationId();

        // Assert
        assertThat(extracted).isNull();
    }

    @Test
    @DisplayName("Deve extrair causationId do metadata")
    void testGetCausationId_ReturnsFromMetadata() {
        // Arrange
        String causationId = UUID.randomUUID().toString();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("causationId", causationId);

        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(metadata)
                .payload("test payload")
                .build();

        // Act
        String extracted = envelope.getCausationId();

        // Assert
        assertThat(extracted).isEqualTo(causationId);
    }

    @Test
    @DisplayName("Deve extrair traceparent do metadata")
    void testGetTraceparent_ReturnsFromMetadata() {
        // Arrange
        String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("traceparent", traceparent);

        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(metadata)
                .payload("test payload")
                .build();

        // Act
        String extracted = envelope.getTraceparent();

        // Assert
        assertThat(extracted).isEqualTo(traceparent);
    }

    @Test
    @DisplayName("Deve criar novo envelope com payload diferente (withPayload)")
    void testWithPayload_CreatesNewEnvelope() {
        // Arrange
        EventEnvelope<String> original = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("original payload")
                .build();

        // Act
        EventEnvelope<Integer> transformed = original.withPayload(42);

        // Assert
        assertThat(transformed.getPayload()).isEqualTo(42);
        assertThat(transformed.getEventId()).isEqualTo(original.getEventId());
        assertThat(transformed.getType()).isEqualTo(original.getType());
        assertThat(transformed.getVersion()).isEqualTo(original.getVersion());
        assertThat(transformed.getOccurredAt()).isEqualTo(original.getOccurredAt());
        assertThat(transformed.getProducer()).isEqualTo(original.getProducer());
        assertThat(transformed.getMetadata()).isEqualTo(original.getMetadata());
    }

    @Test
    @DisplayName("Deve validar envelope mesmo quando metadata é null")
    void testIsValid_NullMetadata_StillValidIfOtherFieldsPresent() {
        // Arrange
        EventEnvelope<String> envelope = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("test_event")
                .version(1)
                .occurredAt(Instant.now())
                .producer("cinelog")
                .metadata(null) // Null metadata é OK
                .payload("test payload")
                .build();

        // Act
        boolean valid = envelope.isValid();

        // Assert
        assertThat(valid).isTrue(); // Metadata é opcional
    }
}
