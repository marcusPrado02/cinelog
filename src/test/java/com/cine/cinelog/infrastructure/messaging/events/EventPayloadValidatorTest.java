package com.cine.cinelog.infrastructure.messaging.events;

import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de segurança para EventPayloadValidator.
 * <p>
 * Garante que eventos não contenham dados sensíveis (PII).
 * <p>
 * PR5: Event Security - PII Protection
 */
@DisplayName("EventPayloadValidator - Security Tests")
class EventPayloadValidatorTest {

    private EventPayloadValidator validator;

    @BeforeEach
    void setUp() {
        validator = new EventPayloadValidator();
    }

    @Test
    @DisplayName("Deve validar payload seguro sem exceção")
    void shouldValidateSafePayloadWithoutException() {
        // Arrange
        WatchEntryCreatedEvent safeEvent = new WatchEntryCreatedEvent(
                UUID.randomUUID(),
                Instant.now(),
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                new BigDecimal("8.5"));

        // Act & Assert - Não deve lançar exceção
        validator.validatePayload(safeEvent, "watchentry.created");
    }

    @Test
    @DisplayName("Deve rejeitar payload com campo 'password'")
    void shouldRejectPayloadWithPasswordField() {
        // Arrange
        String maliciousPayload = "WatchEntryCreatedEvent{password=secret123, userId=1}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(maliciousPayload, "test.event"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("forbidden sensitive field");
    }

    @Test
    @DisplayName("Deve rejeitar payload com campo 'token'")
    void shouldRejectPayloadWithTokenField() {
        // Arrange
        String maliciousPayload = "Event{token=abc123xyz, data=test}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(maliciousPayload, "test.event"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("token");
    }

    @Test
    @DisplayName("Deve rejeitar payload com campo 'secret'")
    void shouldRejectPayloadWithSecretField() {
        // Arrange
        String maliciousPayload = "Event{secret=my-secret-key, userId=1}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(maliciousPayload, "test.event"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("secret");
    }

    @Test
    @DisplayName("Deve rejeitar payload com campo 'apiKey'")
    void shouldRejectPayloadWithApiKeyField() {
        // Arrange
        String maliciousPayload = "Event{apiKey=sk_live_123456, userId=1}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(maliciousPayload, "test.event"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("apikey");
    }

    @Test
    @DisplayName("Deve rejeitar payload com campo 'creditCard'")
    void shouldRejectPayloadWithCreditCardField() {
        // Arrange
        String maliciousPayload = "Event{creditCard=4111-1111-1111-1111, userId=1}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(maliciousPayload, "test.event"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("creditcard");
    }

    @Test
    @DisplayName("Deve truncar string longa corretamente")
    void shouldTruncateLongStringCorrectly() {
        // Arrange
        String longString = "A".repeat(300);
        int maxLength = 200;

        // Act
        String truncated = validator.truncate(longString, maxLength);

        // Assert
        assertThat(truncated).hasSize(maxLength);
        assertThat(truncated).endsWith("...");
    }

    @Test
    @DisplayName("Não deve truncar string curta")
    void shouldNotTruncateShortString() {
        // Arrange
        String shortString = "Short text";
        int maxLength = 200;

        // Act
        String result = validator.truncate(shortString, maxLength);

        // Assert
        assertThat(result).isEqualTo(shortString);
    }

    @Test
    @DisplayName("Deve retornar null para string null no truncate")
    void shouldReturnNullForNullStringInTruncate() {
        // Act
        String result = validator.truncate(null, 100);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve mascarar string corretamente")
    void shouldMaskStringCorrectly() {
        // Arrange
        String email = "user@example.com";
        int visibleChars = 2;

        // Act
        String masked = validator.mask(email, visibleChars);

        // Assert
        assertThat(masked).startsWith("us");
        assertThat(masked).endsWith("om");
        assertThat(masked).contains("*");
    }

    @Test
    @DisplayName("Não deve mascarar string curta")
    void shouldNotMaskShortString() {
        // Arrange
        String shortString = "abc";
        int visibleChars = 2;

        // Act
        String result = validator.mask(shortString, visibleChars);

        // Assert
        assertThat(result).isEqualTo(shortString);
    }

    @Test
    @DisplayName("Deve retornar null para string null no mask")
    void shouldReturnNullForNullStringInMask() {
        // Act
        String result = validator.mask(null, 2);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar payload null")
    void shouldRejectNullPayload() {
        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(null, "test.event"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Deve retornar padrões sensíveis para documentação")
    void shouldReturnSensitivePatternsForDocumentation() {
        // Act
        var sensitivePatterns = validator.getSensitivePatterns();

        // Assert
        assertThat(sensitivePatterns).isNotEmpty();
        assertThat(sensitivePatterns).contains("password", "token", "secret", "apikey");
    }

    @Test
    @DisplayName("Deve retornar padrões de PII para documentação")
    void shouldReturnPiiPatternsForDocumentation() {
        // Act
        var piiPatterns = validator.getPiiPatterns();

        // Assert
        assertThat(piiPatterns).isNotEmpty();
        assertThat(piiPatterns).contains("email", "phone", "address");
    }

    @Test
    @DisplayName("Validação deve ser case-insensitive")
    void shouldBeCaseInsensitive() {
        // Arrange
        String payload1 = "Event{PASSWORD=secret}";
        String payload2 = "Event{PaSsWoRd=secret}";
        String payload3 = "Event{password=secret}";

        // Act & Assert - Todos devem ser rejeitados
        assertThatThrownBy(() -> validator.validatePayload(payload1, "test"))
                .isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> validator.validatePayload(payload2, "test"))
                .isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> validator.validatePayload(payload3, "test"))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    @DisplayName("Deve detectar variações de 'api_key'")
    void shouldDetectApiKeyVariations() {
        // Arrange
        String payload1 = "Event{api_key=123}";
        String payload2 = "Event{apiKey=123}";
        String payload3 = "Event{apikey=123}";

        // Act & Assert
        assertThatThrownBy(() -> validator.validatePayload(payload1, "test"))
                .isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> validator.validatePayload(payload2, "test"))
                .isInstanceOf(SecurityException.class);

        assertThatThrownBy(() -> validator.validatePayload(payload3, "test"))
                .isInstanceOf(SecurityException.class);
    }
}
