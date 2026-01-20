package com.cine.cinelog.core.domain.validator.impl;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para {@link SeriesValidator}.
 *
 * <p>
 * Valida a implementação do Template Method Pattern para séries:
 * <ul>
 * <li>Ano obrigatório (diferente de filmes clássicos)</li>
 * <li>Validações específicas de séries de TV</li>
 * <li>Título original para séries estrangeiras</li>
 * <li>Normalização pós-validação</li>
 * </ul>
 */
@DisplayName("SeriesValidator - Template Method Pattern")
class SeriesValidatorTest {

    private SeriesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SeriesValidator();
    }

    @Nested
    @DisplayName("Ano de Lançamento (Obrigatório)")
    class ReleaseYearTests {

        @Test
        @DisplayName("Deve validar série com ano válido")
        void shouldValidateSeriesWithValidYear() {
            // Arrange
            Media series = new Media(
                    null,
                    "Breaking Bad",
                    MediaType.SERIES,
                    2008,
                    "Breaking Bad",
                    "en",
                    "https://poster.url",
                    "https://backdrop.url",
                    "A high school chemistry teacher turned meth cook...",
                    67890L);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve REJEITAR série sem ano (obrigatório)")
        void shouldRejectSeriesWithoutYear() {
            // Arrange
            Media series = new Media(
                    null,
                    "Series Without Year",
                    MediaType.SERIES,
                    null, // ❌ Obrigatório para séries!
                    "Series Without Year",
                    "en",
                    null,
                    null,
                    "A TV series...",
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("required"));
            assertTrue(ex.getMessage().contains("series") || ex.getMessage().contains("TV"));
        }

        @Test
        @DisplayName("Deve aceitar série atual")
        void shouldAcceptCurrentYearSeries() {
            // Arrange
            int currentYear = java.time.Year.now().getValue();
            Media series = new Media(
                    null,
                    "Current Series",
                    MediaType.SERIES,
                    currentYear,
                    "Current Series",
                    "en",
                    null,
                    null,
                    "A new TV series airing now...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve aceitar série anunciada para próximos 3 anos")
        void shouldAcceptAnnouncedSeriesUpToThreeYears() {
            // Arrange
            int currentYear = java.time.Year.now().getValue();
            Media series = new Media(
                    null,
                    "Future Series",
                    MediaType.SERIES,
                    currentYear + 3, // Máximo permitido
                    "Future Series",
                    "en",
                    null,
                    null,
                    "An upcoming TV series...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve rejeitar série com ano futuro distante")
        void shouldRejectSeriesWithFarFutureYear() {
            // Arrange
            int currentYear = java.time.Year.now().getValue();
            Media series = new Media(
                    null,
                    "Far Future Series",
                    MediaType.SERIES,
                    currentYear + 10, // Muito no futuro
                    "Far Future Series",
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("too far in the future"));
        }

        @Test
        @DisplayName("Deve rejeitar série com ano anterior a 1950")
        void shouldRejectSeriesWithYearBeforeTVCommercial() {
            // Arrange
            Media series = new Media(
                    null,
                    "Pre-TV Series",
                    MediaType.SERIES,
                    1940, // Antes da TV comercial
                    "Pre-TV Series",
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("1950") || ex.getMessage().contains("too old"));
        }

        @Test
        @DisplayName("Deve aceitar série do início da TV (1950)")
        void shouldAcceptEarlyTVSeries() {
            // Arrange
            Media series = new Media(
                    null,
                    "I Love Lucy",
                    MediaType.SERIES,
                    1951, // Início da TV comercial
                    "I Love Lucy",
                    "en",
                    null,
                    null,
                    "Classic sitcom from the 1950s...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }
    }

    @Nested
    @DisplayName("Título Original para Séries Estrangeiras")
    class OriginalTitleForForeignSeriesTests {

        @Test
        @DisplayName("Deve aceitar série em inglês sem título original")
        void shouldAcceptEnglishSeriesWithoutOriginalTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    "Breaking Bad",
                    MediaType.SERIES,
                    2008,
                    null, // Título original opcional para inglês
                    "en",
                    null,
                    null,
                    "An American series...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve REJEITAR série estrangeira sem título original")
        void shouldRejectForeignSeriesWithoutOriginalTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    "Squid Game",
                    MediaType.SERIES,
                    2021,
                    null, // ❌ Obrigatório para idioma não-inglês!
                    "ko", // Coreano
                    null,
                    null,
                    "456 desperate contestants compete...",
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("Original title"));
            assertTrue(ex.getMessage().contains("required"));
            assertTrue(ex.getMessage().contains("ko"));
        }

        @Test
        @DisplayName("Deve aceitar série estrangeira com título original")
        void shouldAcceptForeignSeriesWithOriginalTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    "Squid Game",
                    MediaType.SERIES,
                    2021,
                    "오징어 게임", // Título original em coreano
                    "ko",
                    null,
                    null,
                    "456 desperate contestants compete...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve aceitar série espanhola com título original")
        void shouldAcceptSpanishSeriesWithOriginalTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    "Money Heist",
                    MediaType.SERIES,
                    2017,
                    "La Casa de Papel", // Título original em espanhol
                    "es",
                    null,
                    null,
                    "A criminal mastermind plans heists...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve aceitar série japonesa com título original")
        void shouldAcceptJapaneseSeriesWithOriginalTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    "Attack on Titan",
                    MediaType.SERIES,
                    2013,
                    "進撃の巨人", // Shingeki no Kyojin
                    "ja",
                    null,
                    null,
                    "Humanity fights against giant humanoid creatures...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }
    }

    @Nested
    @DisplayName("Overview (Recomendada mas não obrigatória)")
    class OverviewRecommendationTests {

        @Test
        @DisplayName("Deve aceitar série sem overview (não obrigatória)")
        void shouldAcceptSeriesWithoutOverview() {
            // Arrange
            Media series = new Media(
                    null,
                    "No Overview Series",
                    MediaType.SERIES,
                    2020,
                    "No Overview Series",
                    "en",
                    null,
                    null,
                    null, // Sem overview
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve aceitar série com overview curta (< 50 chars)")
        void shouldAcceptSeriesWithShortOverview() {
            // Arrange
            Media series = new Media(
                    null,
                    "Short Overview Series",
                    MediaType.SERIES,
                    2021,
                    null,
                    "en",
                    null,
                    null,
                    "A TV show", // Menos de 50 caracteres
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }

        @Test
        @DisplayName("Deve aceitar série com overview adequada (>= 50 chars)")
        void shouldAcceptSeriesWithAdequateOverview() {
            // Arrange
            Media series = new Media(
                    null,
                    "Good Overview Series",
                    MediaType.SERIES,
                    2022,
                    null,
                    "en",
                    null,
                    null,
                    "A comprehensive description of a TV series that provides enough context for viewers to understand the plot", // >
                                                                                                                                  // 50
                                                                                                                                  // chars
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(series));
        }
    }

    @Nested
    @DisplayName("Pós-Validação (Normalização)")
    class PostValidationTests {

        @Test
        @DisplayName("Deve normalizar título com espaços extras")
        void shouldNormalizeTitleWithExtraSpaces() {
            // Arrange
            Media series = new Media(
                    null,
                    "  Breaking   Bad    ",
                    MediaType.SERIES,
                    2008,
                    null,
                    "en",
                    null,
                    null,
                    "A TV series...",
                    null);

            // Act
            validator.validate(series);

            // Assert
            assertEquals("Breaking Bad", series.getTitle());
        }

        @Test
        @DisplayName("Deve normalizar título original com espaços extras")
        void shouldNormalizeOriginalTitleWithExtraSpaces() {
            // Arrange
            Media series = new Media(
                    null,
                    "Money Heist",
                    MediaType.SERIES,
                    2017,
                    "  La  Casa  de  Papel  ",
                    "es",
                    null,
                    null,
                    "A criminal mastermind...",
                    null);

            // Act
            validator.validate(series);

            // Assert
            assertEquals("La Casa de Papel", series.getOriginalTitle());
        }

        @Test
        @DisplayName("Deve normalizar idioma para lowercase")
        void shouldNormalizeLanguageToLowercase() {
            // Arrange
            Media series = new Media(
                    null,
                    "Breaking Bad",
                    MediaType.SERIES,
                    2008,
                    "Breaking Bad",
                    "EN", // Uppercase
                    null,
                    null,
                    "A TV series...",
                    null);

            // Act
            validator.validate(series);

            // Assert
            assertEquals("en", series.getOriginalLanguage());
        }

        @Test
        @DisplayName("Deve truncar overview muito longa (> 5000 chars)")
        void shouldTruncateLongOverview() {
            // Arrange
            String longOverview = "A".repeat(5100); // 5100 caracteres
            Media series = new Media(
                    null,
                    "Long Overview Series",
                    MediaType.SERIES,
                    2020,
                    null,
                    "en",
                    null,
                    null,
                    longOverview,
                    null);

            // Act
            validator.validate(series);

            // Assert
            assertNotNull(series.getOverview());
            assertEquals(5000, series.getOverview().length());
            assertTrue(series.getOverview().endsWith("..."));
        }
    }

    @Nested
    @DisplayName("Validações Comuns (Herdadas de AbstractMediaValidator)")
    class CommonValidationsTests {

        @Test
        @DisplayName("Deve rejeitar série sem título")
        void shouldRejectSeriesWithoutTitle() {
            // Arrange
            Media series = new Media(
                    null,
                    null, // Sem título
                    MediaType.SERIES,
                    2020,
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("Title"));
        }

        @Test
        @DisplayName("Deve rejeitar série sem tipo")
        void shouldRejectSeriesWithoutType() {
            // Arrange
            Media series = new Media(
                    null,
                    "No Type Series",
                    null, // Sem tipo
                    2020,
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(series));
            assertTrue(ex.getMessage().contains("type"));
        }
    }

    @Nested
    @DisplayName("Identificador do Tipo")
    class MediaTypeIdentifierTests {

        @Test
        @DisplayName("Deve retornar 'SERIES' como identificador do tipo")
        void shouldReturnSeriesAsTypeIdentifier() {
            // Act
            String typeName = validator.getMediaTypeName();

            // Assert
            assertEquals("SERIES", typeName);
        }
    }
}
