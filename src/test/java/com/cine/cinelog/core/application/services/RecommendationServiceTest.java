package com.cine.cinelog.core.application.services;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.strategy.impl.CollaborativeRecommendationStrategy;
import com.cine.cinelog.core.domain.strategy.impl.ContentBasedRecommendationStrategy;
import com.cine.cinelog.core.domain.strategy.impl.HybridRecommendationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Tests")
class RecommendationServiceTest {

    @Mock
    private ContentBasedRecommendationStrategy contentBasedStrategy;

    @Mock
    private CollaborativeRecommendationStrategy collaborativeStrategy;

    @Mock
    private HybridRecommendationStrategy hybridStrategy;

    @InjectMocks
    private RecommendationService recommendationService;

    private Media media1;
    private Media media2;
    private Media media3;

    @BeforeEach
    void setUp() {
        media1 = createMedia(1L, "Inception", MediaType.MOVIE, 2010);
        media2 = createMedia(2L, "The Matrix", MediaType.MOVIE, 1999);
        media3 = createMedia(3L, "Breaking Bad", MediaType.SERIES, 2008);
    }

    private Media createMedia(Long id, String title, MediaType type, int year) {
        Media media = new Media();
        media.setId(id);
        media.setTitle(title);
        media.setType(type);
        media.setReleaseYear(year);
        media.setPosterUrl("/poster" + id + ".jpg");
        return media;
    }

    // ========== Strategy Selection Tests ==========

    @Test
    @DisplayName("Should use Hybrid strategy when available")
    void shouldUseHybridStrategyWhenAvailable() {
        // Arrange
        Long userId = 1L;
        int limit = 20;
        List<Media> expectedRecommendations = Arrays.asList(media1, media2);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).isApplicable(userId);
        verify(hybridStrategy).recommend(userId, limit);
        verifyNoInteractions(contentBasedStrategy, collaborativeStrategy);
    }

    @Test
    @DisplayName("Should fallback to ContentBased when Hybrid not available")
    void shouldFallbackToContentBasedWhenHybridNotAvailable() {
        // Arrange
        Long userId = 1L;
        int limit = 20;
        List<Media> expectedRecommendations = Arrays.asList(media1, media3);

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);
        when(contentBasedStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(contentBasedStrategy).recommend(userId, limit);
        verifyNoInteractions(collaborativeStrategy);
    }

    @Test
    @DisplayName("Should fallback to Collaborative when Hybrid and ContentBased not available")
    void shouldFallbackToCollaborativeWhenOthersNotAvailable() {
        // Arrange
        Long userId = 1L;
        int limit = 20;
        List<Media> expectedRecommendations = List.of(media2);

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(false);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(true);
        when(collaborativeStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(collaborativeStrategy).isApplicable(userId);
        verify(collaborativeStrategy).recommend(userId, limit);
    }

    @Test
    @DisplayName("Should return empty list when no strategy available")
    void shouldReturnEmptyListWhenNoStrategyAvailable() {
        // Arrange
        Long userId = 1L;
        int limit = 20;

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(false);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(false);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isEmpty();
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(collaborativeStrategy).isApplicable(userId);
        verifyNoMoreInteractions(hybridStrategy, contentBasedStrategy, collaborativeStrategy);
    }

    // ========== Specific Strategy Tests ==========

    @Test
    @DisplayName("Should use ContentBased strategy when explicitly requested")
    void shouldUseContentBasedStrategyWhenExplicitlyRequested() {
        // Arrange
        Long userId = 1L;
        String strategyName = "content-based";
        int limit = 15;
        List<Media> expectedRecommendations = Arrays.asList(media1, media2, media3);

        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);
        when(contentBasedStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendationsByStrategy(userId, strategyName, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(contentBasedStrategy).recommend(userId, limit);
        verifyNoInteractions(hybridStrategy, collaborativeStrategy);
    }

    @Test
    @DisplayName("Should use Collaborative strategy when explicitly requested")
    void shouldUseCollaborativeStrategyWhenExplicitlyRequested() {
        // Arrange
        Long userId = 2L;
        String strategyName = "collaborative";
        int limit = 10;
        List<Media> expectedRecommendations = List.of(media1);

        when(collaborativeStrategy.isApplicable(userId)).thenReturn(true);
        when(collaborativeStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendationsByStrategy(userId, strategyName, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(collaborativeStrategy).isApplicable(userId);
        verify(collaborativeStrategy).recommend(userId, limit);
        verifyNoInteractions(hybridStrategy, contentBasedStrategy);
    }

    @Test
    @DisplayName("Should use Hybrid strategy when explicitly requested")
    void shouldUseHybridStrategyWhenExplicitlyRequested() {
        // Arrange
        Long userId = 3L;
        String strategyName = "hybrid";
        int limit = 25;
        List<Media> expectedRecommendations = Arrays.asList(media1, media2);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendationsByStrategy(userId, strategyName, limit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).isApplicable(userId);
        verify(hybridStrategy).recommend(userId, limit);
        verifyNoInteractions(contentBasedStrategy, collaborativeStrategy);
    }

    @Test
    @DisplayName("Should throw exception for invalid strategy name")
    void shouldThrowExceptionForInvalidStrategyName() {
        // Arrange
        Long userId = 1L;
        String invalidStrategy = "invalid-strategy";
        int limit = 20;

        // Act & Assert
        assertThatThrownBy(() -> recommendationService.getRecommendationsByStrategy(userId, invalidStrategy, limit))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Estratégia desconhecida");
    }

    @Test
    @DisplayName("Should return empty list when specific strategy not applicable")
    void shouldReturnEmptyListWhenSpecificStrategyNotApplicable() {
        // Arrange
        Long userId = 1L;
        String strategyName = "content-based";
        int limit = 20;

        when(contentBasedStrategy.isApplicable(userId)).thenReturn(false);

        // Act
        List<Media> result = recommendationService.getRecommendationsByStrategy(userId, strategyName, limit);

        // Assert
        assertThat(result).isEmpty();
        verify(contentBasedStrategy).isApplicable(userId);
        verifyNoMoreInteractions(contentBasedStrategy);
    }

    // ========== Limit Validation Tests ==========

    @Test
    @DisplayName("Should use default limit of 20 when null provided")
    void shouldUseDefaultLimitWhenNullProvided() {
        // Arrange
        Long userId = 1L;
        Integer nullLimit = null;
        int expectedLimit = 20;
        List<Media> expectedRecommendations = List.of(media1);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, expectedLimit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, nullLimit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).recommend(userId, expectedLimit);
    }

    @Test
    @DisplayName("Should accept minimum limit of 1")
    void shouldAcceptMinimumLimitOf1() {
        // Arrange
        Long userId = 1L;
        int limit = 1;
        List<Media> expectedRecommendations = List.of(media1);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).hasSize(1);
        verify(hybridStrategy).recommend(userId, limit);
    }

    @Test
    @DisplayName("Should accept maximum limit of 100")
    void shouldAcceptMaximumLimitOf100() {
        // Arrange
        Long userId = 1L;
        int limit = 100;
        List<Media> expectedRecommendations = Arrays.asList(media1, media2, media3);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(expectedRecommendations);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isNotEmpty();
        verify(hybridStrategy).recommend(userId, limit);
    }

    @Test
    @DisplayName("Should adjust limit to minimum when zero")
    void shouldAdjustLimitToMinimumWhenZero() {
        // Arrange
        Long userId = 1L;
        int invalidLimit = 0;
        List<Media> expectedRecommendations = List.of(media1);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, 1)).thenReturn(expectedRecommendations); // Ajustado para 1

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, invalidLimit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).recommend(userId, 1); // Verificar que foi ajustado
    }

    @Test
    @DisplayName("Should adjust limit to minimum when negative")
    void shouldAdjustLimitToMinimumWhenNegative() {
        // Arrange
        Long userId = 1L;
        int invalidLimit = -5;
        List<Media> expectedRecommendations = List.of(media1);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, 1)).thenReturn(expectedRecommendations); // Ajustado para 1

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, invalidLimit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).recommend(userId, 1); // Verificar que foi ajustado
    }

    @Test
    @DisplayName("Should adjust limit to maximum when exceeds 100")
    void shouldAdjustLimitToMaximumWhenExceeds100() {
        // Arrange
        Long userId = 1L;
        int invalidLimit = 101;
        List<Media> expectedRecommendations = List.of(media1, media2);

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, 100)).thenReturn(expectedRecommendations); // Ajustado para 100

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, invalidLimit);

        // Assert
        assertThat(result).isEqualTo(expectedRecommendations);
        verify(hybridStrategy).recommend(userId, 100); // Verificar que foi ajustado
    }

    // ========== hasRecommendations Tests ==========

    @Test
    @DisplayName("Should return true when at least one strategy is applicable")
    void shouldReturnTrueWhenAtLeastOneStrategyIsApplicable() {
        // Arrange
        Long userId = 1L;

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);

        // Act
        boolean result = recommendationService.hasRecommendations(userId);

        // Assert
        assertThat(result).isTrue();
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verifyNoInteractions(collaborativeStrategy);
    }

    @Test
    @DisplayName("Should return false when no strategy is applicable")
    void shouldReturnFalseWhenNoStrategyIsApplicable() {
        // Arrange
        Long userId = 1L;

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(false);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(false);

        // Act
        boolean result = recommendationService.hasRecommendations(userId);

        // Assert
        assertThat(result).isFalse();
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(collaborativeStrategy).isApplicable(userId);
    }

    // ========== getAvailableStrategies Tests ==========

    @Test
    @DisplayName("Should return all strategies when all are applicable")
    void shouldReturnAllStrategiesWhenAllAreApplicable() {
        // Arrange
        Long userId = 1L;

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(true);

        // Act
        List<String> result = recommendationService.getAvailableStrategies(userId);

        // Assert
        assertThat(result)
                .hasSize(3)
                .containsExactlyInAnyOrder("hybrid", "content-based", "collaborative");

        // Verify interactions
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(collaborativeStrategy).isApplicable(userId);
    }

    @Test
    @DisplayName("Should return only applicable strategies")
    void shouldReturnOnlyApplicableStrategies() {
        // Arrange
        Long userId = 1L;

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(true);

        // Act
        List<String> result = recommendationService.getAvailableStrategies(userId);

        // Assert
        assertThat(result)
                .hasSize(2)
                .containsExactlyInAnyOrder("content-based", "collaborative");
        verify(hybridStrategy).isApplicable(userId);
        verify(contentBasedStrategy).isApplicable(userId);
        verify(collaborativeStrategy).isApplicable(userId);
    }

    @Test
    @DisplayName("Should return empty list when no strategies are applicable")
    void shouldReturnEmptyListWhenNoStrategiesAreApplicable() {
        // Arrange
        Long userId = 1L;

        when(hybridStrategy.isApplicable(userId)).thenReturn(false);
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(false);
        when(collaborativeStrategy.isApplicable(userId)).thenReturn(false);

        // Act
        List<String> result = recommendationService.getAvailableStrategies(userId);

        // Assert
        assertThat(result).isEmpty();
        verify(hybridStrategy, never()).getStrategyName();
        verify(contentBasedStrategy, never()).getStrategyName();
        verify(collaborativeStrategy, never()).getStrategyName();
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Should handle strategy returning empty list")
    void shouldHandleStrategyReturningEmptyList() {
        // Arrange
        Long userId = 1L;
        int limit = 20;

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(Collections.emptyList());

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isEmpty();
        verify(hybridStrategy).recommend(userId, limit);
    }

    @Test
    @DisplayName("Should return null when strategy returns null (no fallback)")
    void shouldReturnNullWhenStrategyReturnsNull() {
        // Arrange
        Long userId = 1L;
        int limit = 20;

        when(hybridStrategy.isApplicable(userId)).thenReturn(true);
        when(hybridStrategy.recommend(userId, limit)).thenReturn(null);

        // Act
        List<Media> result = recommendationService.getRecommendations(userId, limit);

        // Assert
        assertThat(result).isNull(); // Serviço retorna null, não faz fallback
        verify(hybridStrategy).recommend(userId, limit);
        verifyNoInteractions(contentBasedStrategy, collaborativeStrategy);
    }

    @Test
    @DisplayName("Should handle different limits for specific strategy")
    void shouldHandleDifferentLimitsForSpecificStrategy() {
        // Arrange
        Long userId = 1L;
        String strategyName = "content-based";

        // Test with limit 5
        when(contentBasedStrategy.isApplicable(userId)).thenReturn(true);
        when(contentBasedStrategy.recommend(userId, 5)).thenReturn(List.of(media1));

        List<Media> result1 = recommendationService.getRecommendationsByStrategy(userId, strategyName, 5);
        assertThat(result1).hasSize(1);

        // Test with limit 50
        when(contentBasedStrategy.recommend(userId, 50)).thenReturn(Arrays.asList(media1, media2, media3));

        List<Media> result2 = recommendationService.getRecommendationsByStrategy(userId, strategyName, 50);
        assertThat(result2).hasSize(3);

        verify(contentBasedStrategy).recommend(userId, 5);
        verify(contentBasedStrategy).recommend(userId, 50);
    }
}
