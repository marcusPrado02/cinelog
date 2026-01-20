package com.cine.cinelog.core.domain.validator.impl;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para {@link MovieValidator}.
 *
 * <p>
 * Valida a implementação do Template Method Pattern para filmes:
 * <ul>
 * <li>Validações específicas de filmes (modernos vs clássicos)</li>
 * <li>Normalização pós-validação</li>
 * <li>Recomendações não-obrigatórias</li>
 * </ul>
 */
@DisplayName("MovieValidator - Template Method Pattern")
class MovieValidatorTest {

    private MovieValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MovieValidator();
    }

    @Nested
    @DisplayName("Validações de Filmes Modernos (>= 1960)")
    class ModernMoviesTests {

        @Test
        @DisplayName("Deve validar filme moderno com ano válido")
        void shouldValidateModernMovieWithValidYear() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Inception",
                    MediaType.MOVIE,
                    2010,
                    "Inception",
                    "en",
                    "https://poster.url",
                    "https://backdrop.url",
                    "A thief who steals corporate secrets...",
                    12345L);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }

        @Test
        @DisplayName("Deve rejeitar filme moderno com ano futuro distante")
        void shouldRejectModernMovieWithFarFutureYear() {
            // Arrange
            int currentYear = java.time.Year.now().getValue();
            Media movie = new Media(
                    null,
                    "Future Movie",
                    MediaType.MOVIE,
                    currentYear + 10, // Muito no futuro
                    "Future Movie",
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(movie));
            assertTrue(ex.getMessage().contains("too far in the future"));
        }

        @Test
        @DisplayName("Deve aceitar filme com ano até atual + 5")
        void shouldAcceptMovieWithYearUpToCurrentPlusFive() {
            // Arrange
            int currentYear = java.time.Year.now().getValue();
            Media movie = new Media(
                    null,
                    "Upcoming Movie",
                    MediaType.MOVIE,
                    currentYear + 5, // Limite máximo permitido
                    "Upcoming Movie",
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }
    }

    @Nested
    @DisplayName("Validações de Filmes Clássicos (< 1960)")
    class ClassicMoviesTests {

        @Test
        @DisplayName("Deve validar filme clássico sem restrições rígidas de ano")
        void shouldValidateClassicMovieWithFlexibleYearRules() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Casablanca",
                    MediaType.MOVIE,
                    1942,
                    "Casablanca",
                    "en",
                    null,
                    null,
                    "A classic romance set in WWII...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }

        @Test
        @DisplayName("Deve aceitar filme clássico sem ano (registros históricos)")
        void shouldAcceptClassicMovieWithoutYear() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Old Silent Movie",
                    MediaType.MOVIE,
                    null, // Sem ano (registros antigos podem não ter)
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }

        @Test
        @DisplayName("Deve aceitar filme do início do cinema (1888+)")
        void shouldAcceptEarlyCinemaMovie() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Le Voyage dans la Lune",
                    MediaType.MOVIE,
                    1902, // Georges Méliès
                    "Le Voyage dans la Lune",
                    "fr",
                    null,
                    null,
                    "A Trip to the Moon",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }
    }

    @Nested
    @DisplayName("Recomendações Não-Obrigatórias")
    class RecommendationsTests {

        @Test
        @DisplayName("Deve aceitar filme sem overview (recomendado mas não obrigatório)")
        void shouldAcceptMovieWithoutOverview() {
            // Arrange
            Media movie = new Media(
                    null,
                    "No Overview Movie",
                    MediaType.MOVIE,
                    2020,
                    "No Overview Movie",
                    "en",
                    null,
                    null,
                    null, // Sem overview
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }

        @Test
        @DisplayName("Deve aceitar filme sem título original (recomendado para estrangeiros)")
        void shouldAcceptMovieWithoutOriginalTitle() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Foreign Film",
                    MediaType.MOVIE,
                    2021,
                    null, // Sem título original
                    "ja", // Japonês
                    null,
                    null,
                    "A Japanese movie...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }

        @Test
        @DisplayName("Deve aceitar filme sem TMDB ID (recomendado mas não obrigatório)")
        void shouldAcceptMovieWithoutTmdbId() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Independent Film",
                    MediaType.MOVIE,
                    2022,
                    "Independent Film",
                    "en",
                    null,
                    null,
                    "An independent production...",
                    null // Sem TMDB ID
            );

            // Act & Assert
            assertDoesNotThrow(() -> validator.validate(movie));
        }
    }

    @Nested
    @DisplayName("Pós-Validação (Normalização)")
    class PostValidationTests {

        @Test
        @DisplayName("Deve normalizar título com espaços extras")
        void shouldNormalizeTitleWithExtraSpaces() {
            // Arrange
            Media movie = new Media(
                    null,
                    "  Inception    Extra   Spaces  ",
                    MediaType.MOVIE,
                    2010,
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act
            validator.validate(movie);

            // Assert
            assertEquals("Inception Extra Spaces", movie.getTitle());
        }

        @Test
        @DisplayName("Deve truncar overview muito longa (> 5000 chars)")
        void shouldTruncateLongOverview() {
            // Arrange
            String longOverview = "A".repeat(5100); // 5100 caracteres
            Media movie = new Media(
                    null,
                    "Long Overview Movie",
                    MediaType.MOVIE,
                    2020,
                    null,
                    "en",
                    null,
                    null,
                    longOverview,
                    null);

            // Act
            validator.validate(movie);

            // Assert
            assertNotNull(movie.getOverview());
            assertEquals(5000, movie.getOverview().length());
            assertTrue(movie.getOverview().endsWith("..."));
        }

        @Test
        @DisplayName("Deve normalizar título original com espaços extras")
        void shouldNormalizeOriginalTitleWithExtraSpaces() {
            // Arrange
            Media movie = new Media(
                    null,
                    "The Matrix",
                    MediaType.MOVIE,
                    1999,
                    "  The   Matrix   ",
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act
            validator.validate(movie);

            // Assert
            assertEquals("The Matrix", movie.getOriginalTitle());
        }
    }

    @Nested
    @DisplayName("Validações Comuns (Herdadas de AbstractMediaValidator)")
    class CommonValidationsTests {

        @Test
        @DisplayName("Deve rejeitar filme sem título")
        void shouldRejectMovieWithoutTitle() {
            // Arrange
            Media movie = new Media(
                    null,
                    null, // Sem título
                    MediaType.MOVIE,
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
                    () -> validator.validate(movie));
            assertTrue(ex.getMessage().contains("Title"));
        }

        @Test
        @DisplayName("Deve rejeitar filme sem tipo")
        void shouldRejectMovieWithoutType() {
            // Arrange
            Media movie = new Media(
                    null,
                    "No Type Movie",
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
                    () -> validator.validate(movie));
            assertTrue(ex.getMessage().contains("type"));
        }

        @Test
        @DisplayName("Deve rejeitar filme com ano muito antigo (< 1888)")
        void shouldRejectMovieWithTooOldYear() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Ancient Movie",
                    MediaType.MOVIE,
                    1800, // Antes do cinema existir
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> validator.validate(movie));
            assertTrue(ex.getMessage().contains("1888") || ex.getMessage().contains("minimum"));
        }
    }

    @Nested
    @DisplayName("Identificador do Tipo")
    class MediaTypeIdentifierTests {

        @Test
        @DisplayName("Deve retornar 'MOVIE' como identificador do tipo")
        void shouldReturnMovieAsTypeIdentifier() {
            // Act
            String typeName = validator.getMediaTypeName();

            // Assert
            assertEquals("MOVIE", typeName);
        }
    }
}
