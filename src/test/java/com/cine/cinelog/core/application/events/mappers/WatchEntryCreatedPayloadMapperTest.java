package com.cine.cinelog.core.application.events.mappers;

import com.cine.cinelog.core.domain.events.watchentry.WatchEntryCreatedEvent;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes para WatchEntryCreatedPayloadMapper.
 * <p>
 * Valida segurança de mapeamento de eventos.
 * <p>
 * PR5: Event Security - PII Protection
 */
@DisplayName("WatchEntryCreatedPayloadMapper - Security Tests")
class WatchEntryCreatedPayloadMapperTest {

    private WatchEntryCreatedPayloadMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new WatchEntryCreatedPayloadMapper();
    }

    @Test
    @DisplayName("Deve mapear WatchEntry para evento seguro")
    void shouldMapWatchEntryToSafeEvent() {
        // Arrange
        WatchEntry watchEntry = new WatchEntry(
                1L, // id
                100L, // userId
                200L, // mediaId
                null, // episodeId
                new BigDecimal("8.5"), // rating
                null, // comment
                LocalDate.of(2026, 1, 11), // watchedAt
                null, // createdAt
                null, // updatedAt
                null, // createdBy
                null, // updatedBy
                null // version
        );

        // Act
        WatchEntryCreatedEvent event = mapper.toEventPayload(watchEntry);

        // Assert
        assertThat(event).isNotNull();
        assertThat(event.watchEntryId()).isEqualTo(1L);
        assertThat(event.userId()).isEqualTo(100L);
        assertThat(event.mediaId()).isEqualTo(200L);
        assertThat(event.episodeId()).isNull();
        assertThat(event.watchedAt()).isEqualTo(LocalDate.of(2026, 1, 11));
        assertThat(event.rating()).isEqualByComparingTo(new BigDecimal("8.5"));
    }

    @Test
    @DisplayName("Deve mapear WatchEntry com episodeId")
    void shouldMapWatchEntryWithEpisodeId() {
        // Arrange
        WatchEntry watchEntry = new WatchEntry(
                1L, // id
                100L, // userId
                null, // mediaId
                300L, // episodeId
                null, // rating
                null, // comment
                LocalDate.of(2026, 1, 11), // watchedAt
                null, null, null, null, null);

        // Act
        WatchEntryCreatedEvent event = mapper.toEventPayload(watchEntry);

        // Assert
        assertThat(event.episodeId()).isEqualTo(300L);
        assertThat(event.mediaId()).isNull();
    }

    @Test
    @DisplayName("Deve rejeitar WatchEntry null")
    void shouldRejectNullWatchEntry() {
        // Act & Assert
        assertThatThrownBy(() -> mapper.toEventPayload(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Deve validar evento com sucesso")
    void shouldValidateEventSuccessfully() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                new BigDecimal("7.0"));

        // Act & Assert - Não deve lançar exceção
        mapper.validatePayload(event);
    }

    @Test
    @DisplayName("Deve rejeitar evento com payload null")
    void shouldRejectEventWithNullPayload() {
        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Deve rejeitar evento sem watchEntryId")
    void shouldRejectEventWithoutWatchEntryId() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                null, // watchEntryId null
                100L,
                200L,
                null,
                LocalDate.now(),
                null);

        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("WatchEntry ID cannot be null");
    }

    @Test
    @DisplayName("Deve rejeitar evento sem userId")
    void shouldRejectEventWithoutUserId() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                null, // userId null
                200L,
                null,
                LocalDate.now(),
                null);

        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("Deve rejeitar evento sem mediaId nem episodeId")
    void shouldRejectEventWithoutMediaIdNorEpisodeId() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                null, // mediaId null
                null, // episodeId null
                LocalDate.now(),
                null);

        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one of mediaId or episodeId");
    }

    @Test
    @DisplayName("Deve rejeitar evento com rating negativo")
    void shouldRejectEventWithNegativeRating() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                new BigDecimal("-1.0") // rating negativo
        );

        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 0 and 10");
    }

    @Test
    @DisplayName("Deve rejeitar evento com rating maior que 10")
    void shouldRejectEventWithRatingGreaterThan10() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                new BigDecimal("11.0") // rating > 10
        );

        // Act & Assert
        assertThatThrownBy(() -> mapper.validatePayload(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rating must be between 0 and 10");
    }

    @Test
    @DisplayName("Deve aceitar rating 0")
    void shouldAcceptRatingZero() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                BigDecimal.ZERO);

        // Act & Assert - Não deve lançar exceção
        mapper.validatePayload(event);
    }

    @Test
    @DisplayName("Deve aceitar rating 10")
    void shouldAcceptRatingTen() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                new BigDecimal("10"));

        // Act & Assert - Não deve lançar exceção
        mapper.validatePayload(event);
    }

    @Test
    @DisplayName("Deve aceitar rating null")
    void shouldAcceptNullRating() {
        // Arrange
        WatchEntryCreatedEvent event = WatchEntryCreatedEvent.of(
                1L,
                100L,
                200L,
                null,
                LocalDate.now(),
                null // rating null é válido
        );

        // Act & Assert - Não deve lançar exceção
        mapper.validatePayload(event);
    }

    @Test
    @DisplayName("Deve retornar allowed fields corretos")
    void shouldReturnCorrectAllowedFields() {
        // Act
        String[] allowedFields = mapper.getAllowedFields();

        // Assert
        assertThat(allowedFields).containsExactlyInAnyOrder(
                "watchEntryId",
                "userId",
                "mediaId",
                "episodeId",
                "watchedAt",
                "rating");
    }

    @Test
    @DisplayName("Deve retornar forbidden fields corretos")
    void shouldReturnCorrectForbiddenFields() {
        // Act
        var forbiddenFields = mapper.getForbiddenFields();

        // Assert
        assertThat(forbiddenFields).contains(
                "email",
                "password",
                "token",
                "secret",
                "apiKey");
    }

    @Test
    @DisplayName("Evento não deve conter campos sensíveis no toString")
    void shouldNotContainSensitiveFieldsInToString() {
        // Arrange
        WatchEntry watchEntry = new WatchEntry(
                1L, 100L, 200L, null, null, null,
                LocalDate.now(), null, null, null, null, null);

        WatchEntryCreatedEvent event = mapper.toEventPayload(watchEntry);

        // Act
        String eventString = event.toString().toLowerCase();

        // Assert - Nenhum campo proibido deve estar presente
        assertThat(eventString).doesNotContain("password");
        assertThat(eventString).doesNotContain("token");
        assertThat(eventString).doesNotContain("secret");
        assertThat(eventString).doesNotContain("apikey");
        assertThat(eventString).doesNotContain("email");
    }
}
