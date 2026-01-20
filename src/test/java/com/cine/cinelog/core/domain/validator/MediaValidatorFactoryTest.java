package com.cine.cinelog.core.domain.validator;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.validator.impl.MovieValidator;
import com.cine.cinelog.core.domain.validator.impl.SeriesValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes para {@link MediaValidatorFactory}.
 *
 * <p>
 * Valida a implementação do Factory Method Pattern para seleção de validadores:
 * <ul>
 * <li>Seleção correta do validador por tipo</li>
 * <li>Singleton behavior (mesma instância)</li>
 * <li>Métodos de conveniência</li>
 * <li>Validação de parâmetros</li>
 * </ul>
 */
@DisplayName("MediaValidatorFactory - Factory Method Pattern")
class MediaValidatorFactoryTest {

    @Nested
    @DisplayName("getValidator(MediaType)")
    class GetValidatorByTypeTests {

        @Test
        @DisplayName("Deve retornar MovieValidator para MediaType.MOVIE")
        void shouldReturnMovieValidatorForMovieType() {
            // Act
            AbstractMediaValidator validator = MediaValidatorFactory.getValidator(MediaType.MOVIE);

            // Assert
            assertNotNull(validator);
            assertInstanceOf(MovieValidator.class, validator);
        }

        @Test
        @DisplayName("Deve retornar SeriesValidator para MediaType.SERIES")
        void shouldReturnSeriesValidatorForSeriesType() {
            // Act
            AbstractMediaValidator validator = MediaValidatorFactory.getValidator(MediaType.SERIES);

            // Assert
            assertNotNull(validator);
            assertInstanceOf(SeriesValidator.class, validator);
        }

        @Test
        @DisplayName("Deve retornar mesma instância (Singleton)")
        void shouldReturnSameInstanceForSameType() {
            // Act
            AbstractMediaValidator validator1 = MediaValidatorFactory.getValidator(MediaType.MOVIE);
            AbstractMediaValidator validator2 = MediaValidatorFactory.getValidator(MediaType.MOVIE);

            // Assert
            assertSame(validator1, validator2, "Deve retornar singleton");
        }

        @Test
        @DisplayName("Deve rejeitar tipo null")
        void shouldRejectNullType() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> MediaValidatorFactory.getValidator((MediaType) null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }
    }

    @Nested
    @DisplayName("getValidator(Media)")
    class GetValidatorByMediaTests {

        @Test
        @DisplayName("Deve retornar MovieValidator para mídia tipo MOVIE")
        void shouldReturnMovieValidatorForMovieMedia() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Inception",
                    MediaType.MOVIE,
                    2010,
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act
            AbstractMediaValidator validator = MediaValidatorFactory.getValidator(movie);

            // Assert
            assertInstanceOf(MovieValidator.class, validator);
        }

        @Test
        @DisplayName("Deve retornar SeriesValidator para mídia tipo SERIES")
        void shouldReturnSeriesValidatorForSeriesMedia() {
            // Arrange
            Media series = new Media(
                    null,
                    "Breaking Bad",
                    MediaType.SERIES,
                    2008,
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act
            AbstractMediaValidator validator = MediaValidatorFactory.getValidator(series);

            // Assert
            assertInstanceOf(SeriesValidator.class, validator);
        }

        @Test
        @DisplayName("Deve rejeitar mídia null")
        void shouldRejectNullMedia() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> MediaValidatorFactory.getValidator((Media) null));
            assertTrue(ex.getMessage().contains("cannot be null"));
        }

        @Test
        @DisplayName("Deve rejeitar mídia com tipo null")
        void shouldRejectMediaWithNullType() {
            // Arrange
            Media media = new Media(
                    null,
                    "No Type Media",
                    null, // Tipo null
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
                    () -> MediaValidatorFactory.getValidator(media));
            assertTrue(ex.getMessage().contains("type cannot be null"));
        }
    }

    @Nested
    @DisplayName("validate(Media) - Método de Conveniência")
    class ValidateMediaTests {

        @Test
        @DisplayName("Deve validar filme usando validador correto")
        void shouldValidateMovieUsingCorrectValidator() {
            // Arrange
            Media movie = new Media(
                    null,
                    "Inception",
                    MediaType.MOVIE,
                    2010,
                    "Inception",
                    "en",
                    null,
                    null,
                    "A thief who steals...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> MediaValidatorFactory.validate(movie));
        }

        @Test
        @DisplayName("Deve validar série usando validador correto")
        void shouldValidateSeriesUsingCorrectValidator() {
            // Arrange
            Media series = new Media(
                    null,
                    "Breaking Bad",
                    MediaType.SERIES,
                    2008,
                    "Breaking Bad",
                    "en",
                    null,
                    null,
                    "A high school chemistry teacher...",
                    null);

            // Act & Assert
            assertDoesNotThrow(() -> MediaValidatorFactory.validate(series));
        }

        @Test
        @DisplayName("Deve propagar exceção de validação de filme")
        void shouldPropagateMovieValidationException() {
            // Arrange - Filme sem título
            Media invalidMovie = new Media(
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
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MediaValidatorFactory.validate(invalidMovie));
        }

        @Test
        @DisplayName("Deve propagar exceção de validação de série")
        void shouldPropagateSeriesValidationException() {
            // Arrange - Série sem ano (obrigatório)
            Media invalidSeries = new Media(
                    null,
                    "Series Without Year",
                    MediaType.SERIES,
                    null, // Sem ano (obrigatório para séries)
                    null,
                    "en",
                    null,
                    null,
                    null,
                    null);

            // Act & Assert
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MediaValidatorFactory.validate(invalidSeries));
        }
    }

    @Nested
    @DisplayName("isSupported(MediaType)")
    class IsSupportedTests {

        @Test
        @DisplayName("Deve confirmar suporte para MOVIE")
        void shouldSupportMovieType() {
            // Act
            boolean supported = MediaValidatorFactory.isSupported(MediaType.MOVIE);

            // Assert
            assertTrue(supported);
        }

        @Test
        @DisplayName("Deve confirmar suporte para SERIES")
        void shouldSupportSeriesType() {
            // Act
            boolean supported = MediaValidatorFactory.isSupported(MediaType.SERIES);

            // Assert
            assertTrue(supported);
        }

        @Test
        @DisplayName("Deve retornar false para tipo null")
        void shouldReturnFalseForNullType() {
            // Act
            boolean supported = MediaValidatorFactory.isSupported(null);

            // Assert
            assertFalse(supported);
        }
    }

    @Nested
    @DisplayName("Comportamento Singleton")
    class SingletonBehaviorTests {

        @Test
        @DisplayName("Deve retornar mesma instância de MovieValidator")
        void shouldReturnSameMovieValidatorInstance() {
            // Act
            AbstractMediaValidator v1 = MediaValidatorFactory.getValidator(MediaType.MOVIE);
            AbstractMediaValidator v2 = MediaValidatorFactory.getValidator(MediaType.MOVIE);
            AbstractMediaValidator v3 = MediaValidatorFactory.getValidator(MediaType.MOVIE);

            // Assert
            assertSame(v1, v2);
            assertSame(v2, v3);
            assertSame(v1, v3);
        }

        @Test
        @DisplayName("Deve retornar mesma instância de SeriesValidator")
        void shouldReturnSameSeriesValidatorInstance() {
            // Act
            AbstractMediaValidator v1 = MediaValidatorFactory.getValidator(MediaType.SERIES);
            AbstractMediaValidator v2 = MediaValidatorFactory.getValidator(MediaType.SERIES);
            AbstractMediaValidator v3 = MediaValidatorFactory.getValidator(MediaType.SERIES);

            // Assert
            assertSame(v1, v2);
            assertSame(v2, v3);
            assertSame(v1, v3);
        }

        @Test
        @DisplayName("MovieValidator e SeriesValidator devem ser instâncias diferentes")
        void shouldHaveDifferentInstancesForDifferentTypes() {
            // Act
            AbstractMediaValidator movieValidator = MediaValidatorFactory.getValidator(MediaType.MOVIE);
            AbstractMediaValidator seriesValidator = MediaValidatorFactory.getValidator(MediaType.SERIES);

            // Assert
            assertNotSame(movieValidator, seriesValidator);
        }
    }

    @Nested
    @DisplayName("Construtor Privado")
    class PrivateConstructorTests {

        @Test
        @DisplayName("Não deve permitir instanciação via reflection")
        void shouldNotAllowInstantiationViaReflection() {
            // Act & Assert
            assertThrows(
                    Exception.class,
                    () -> {
                        var constructor = MediaValidatorFactory.class.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        constructor.newInstance();
                    },
                    "Factory class should not be instantiable");
        }
    }
}
