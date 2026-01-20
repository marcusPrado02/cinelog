package com.cine.cinelog.infrastructure.messaging.events;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários para EventEnvelopeFactory.
 *
 * <p>
 * Valida:
 * <ul>
 * <li>Extração de metadata do MDC (correlationId, traceparent, userId)</li>
 * <li>Geração de correlationId quando ausente no MDC</li>
 * <li>Criação com metadata customizado</li>
 * <li>Causation chain (createCausedBy)</li>
 * <li>Campos padrão (producer, eventId, occurredAt)</li>
 * </ul>
 * </p>
 */
@DisplayName("EventEnvelopeFactory - Envelope Creation with MDC Integration")
class EventEnvelopeFactoryTest {

    private EventEnvelopeFactory factory;

    @BeforeEach
    void setUp() {
        factory = new EventEnvelopeFactory();
        MDC.clear(); // Limpa MDC antes de cada teste
    }

    @AfterEach
    void tearDown() {
        MDC.clear(); // Limpa MDC após cada teste
    }

    @Test
    @DisplayName("Deve extrair correlationId do MDC quando presente")
    void testCreate_ExtractsCorrelationIdFromMDC() {
        // Arrange
        String correlationId = "corr-123-456";
        MDC.put("correlationId", correlationId);

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    @DisplayName("Deve gerar correlationId (UUID) quando ausente no MDC")
    void testCreate_GeneratesCorrelationIdIfMissing() {
        // Arrange - MDC sem correlationId

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getCorrelationId()).isNotNull();
        assertThat(envelope.getCorrelationId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    @DisplayName("Deve extrair traceparent do MDC quando presente")
    void testCreate_ExtractsTraceparentFromMDC() {
        // Arrange
        String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
        MDC.put("traceparent", traceparent);

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getTraceparent()).isEqualTo(traceparent);
    }

    @Test
    @DisplayName("Deve extrair userId do MDC quando presente")
    void testCreate_ExtractsUserIdFromMDC() {
        // Arrange
        String userId = "42";
        MDC.put("userId", userId);

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getMetadata()).containsEntry("userId", userId);
    }

    @Test
    @DisplayName("Deve configurar producer como 'cinelog'")
    void testCreate_SetsProducerToCinelog() {
        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getProducer()).isEqualTo("cinelog");
    }

    @Test
    @DisplayName("Deve gerar eventId único (UUID)")
    void testCreate_GeneratesUniqueEventId() {
        // Act
        EventEnvelope<String> envelope1 = factory.create("test_event", 1, "payload1");
        EventEnvelope<String> envelope2 = factory.create("test_event", 1, "payload2");

        // Assert
        assertThat(envelope1.getEventId()).isNotNull();
        assertThat(envelope2.getEventId()).isNotNull();
        assertThat(envelope1.getEventId()).isNotEqualTo(envelope2.getEventId());
    }

    @Test
    @DisplayName("Deve configurar type e version conforme especificado")
    void testCreate_SetsTypeAndVersion() {
        // Act
        EventEnvelope<String> envelope = factory.create("watch_entry_created", 2, "test payload");

        // Assert
        assertThat(envelope.getType()).isEqualTo("watch_entry_created");
        assertThat(envelope.getVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("Deve configurar payload conforme especificado")
    void testCreate_SetsPayload() {
        // Arrange
        String payload = "test payload content";

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, payload);

        // Assert
        assertThat(envelope.getPayload()).isEqualTo(payload);
    }

    @Test
    @DisplayName("Deve configurar occurredAt (timestamp não nulo)")
    void testCreate_SetsOccurredAt() {
        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.getOccurredAt()).isNotNull();
    }

    @Test
    @DisplayName("Deve criar envelope válido")
    void testCreate_CreatesValidEnvelope() {
        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload");

        // Assert
        assertThat(envelope.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve mesclar metadata customizado com MDC")
    void testCreate_WithCustomMetadata_MergesMDC() {
        // Arrange
        MDC.put("correlationId", "from-mdc");
        Map<String, String> customMetadata = new HashMap<>();
        customMetadata.put("customKey", "customValue");

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload", customMetadata);

        // Assert
        assertThat(envelope.getMetadata()).containsEntry("correlationId", "from-mdc"); // Do MDC
        assertThat(envelope.getMetadata()).containsEntry("customKey", "customValue"); // Customizado
    }

    @Test
    @DisplayName("Deve sobrescrever MDC com metadata customizado quando há conflito")
    void testCreate_CustomMetadataOverridesMDC() {
        // Arrange
        MDC.put("correlationId", "from-mdc");
        Map<String, String> customMetadata = new HashMap<>();
        customMetadata.put("correlationId", "from-custom"); // Conflito

        // Act
        EventEnvelope<String> envelope = factory.create("test_event", 1, "test payload", customMetadata);

        // Assert
        assertThat(envelope.getCorrelationId()).isEqualTo("from-custom"); // Custom tem prioridade
    }

    @Test
    @DisplayName("Deve propagar correlationId do evento causador (createCausedBy)")
    void testCreateCausedBy_PropagatesCorrelationId() {
        // Arrange
        String correlationId = "corr-original";
        EventEnvelope<String> causingEvent = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("causing_event")
                .version(1)
                .occurredAt(java.time.Instant.now())
                .producer("cinelog")
                .metadata(Map.of("correlationId", correlationId))
                .payload("causing payload")
                .build();

        // Act
        EventEnvelope<String> causedEvent = factory.createCausedBy(
                "caused_event",
                1,
                "caused payload",
                causingEvent);

        // Assert
        assertThat(causedEvent.getCorrelationId()).isEqualTo(correlationId); // Propagado
    }

    @Test
    @DisplayName("Deve configurar causationId com eventId do evento causador")
    void testCreateCausedBy_SetsCausationId() {
        // Arrange
        UUID causingEventId = UUID.randomUUID();
        EventEnvelope<String> causingEvent = EventEnvelope.<String>builder()
                .eventId(causingEventId)
                .type("causing_event")
                .version(1)
                .occurredAt(java.time.Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("causing payload")
                .build();

        // Act
        EventEnvelope<String> causedEvent = factory.createCausedBy(
                "caused_event",
                1,
                "caused payload",
                causingEvent);

        // Assert
        assertThat(causedEvent.getCausationId()).isEqualTo(causingEventId.toString());
    }

    @Test
    @DisplayName("Deve propagar traceparent do evento causador")
    void testCreateCausedBy_PropagatesTraceparent() {
        // Arrange
        String traceparent = "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01";
        EventEnvelope<String> causingEvent = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("causing_event")
                .version(1)
                .occurredAt(java.time.Instant.now())
                .producer("cinelog")
                .metadata(Map.of("traceparent", traceparent))
                .payload("causing payload")
                .build();

        // Act
        EventEnvelope<String> causedEvent = factory.createCausedBy(
                "caused_event",
                1,
                "caused payload",
                causingEvent);

        // Assert
        assertThat(causedEvent.getTraceparent()).isEqualTo(traceparent); // Propagado
    }

    @Test
    @DisplayName("Deve criar envelope válido com createCausedBy")
    void testCreateCausedBy_CreatesValidEnvelope() {
        // Arrange
        EventEnvelope<String> causingEvent = EventEnvelope.<String>builder()
                .eventId(UUID.randomUUID())
                .type("causing_event")
                .version(1)
                .occurredAt(java.time.Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("causing payload")
                .build();

        // Act
        EventEnvelope<String> causedEvent = factory.createCausedBy(
                "caused_event",
                1,
                "caused payload",
                causingEvent);

        // Assert
        assertThat(causedEvent.isValid()).isTrue();
    }

    @Test
    @DisplayName("Deve gerar novo eventId para evento causado (não reutilizar do causador)")
    void testCreateCausedBy_GeneratesNewEventId() {
        // Arrange
        UUID causingEventId = UUID.randomUUID();
        EventEnvelope<String> causingEvent = EventEnvelope.<String>builder()
                .eventId(causingEventId)
                .type("causing_event")
                .version(1)
                .occurredAt(java.time.Instant.now())
                .producer("cinelog")
                .metadata(new HashMap<>())
                .payload("causing payload")
                .build();

        // Act
        EventEnvelope<String> causedEvent = factory.createCausedBy(
                "caused_event",
                1,
                "caused payload",
                causingEvent);

        // Assert
        assertThat(causedEvent.getEventId()).isNotNull();
        assertThat(causedEvent.getEventId()).isNotEqualTo(causingEventId); // Novo ID
    }
}
