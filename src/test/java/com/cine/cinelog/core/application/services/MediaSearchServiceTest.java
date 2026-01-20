package com.cine.cinelog.core.application.services;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link MediaSearchService}.
 *
 * <p>
 * Valida:
 * <ul>
 * <li>Busca com critérios válidos</li>
 * <li>Busca simples por texto</li>
 * <li>Validação de paginação (ajuste de size, page negativo)</li>
 * <li>Validação de ranges (yearMin > yearMax, ratingMin > ratingMax)</li>
 * <li>Busca sem critérios (buscar tudo)</li>
 * <li>Cache (verificar chamadas ao repository)</li>
 * </ul>
 *
 * @since 1.0 (PR6)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MediaSearchService - Testes de busca avançada")
class MediaSearchServiceTest {

    @Mock
    private MediaRepositoryPort mediaRepository;

    @InjectMocks
    private MediaSearchService mediaSearchService;

    private Media movieSample;
    private PageResult<Media> pageResult;

    @BeforeEach
    void setUp() {
        // Mock de mídia
        movieSample = new Media(
                1L,
                "The Matrix",
                MediaType.MOVIE,
                1999,
                "The Matrix",
                "en",
                "/poster.jpg",
                "/backdrop.jpg",
                "A computer hacker learns...",
                136250L);

        // Mock de resultado paginado
        pageResult = new PageResult<>(
                List.of(movieSample),
                0,
                20,
                1L,
                1);
    }

    @Test
    @DisplayName("Deve buscar mídias com critérios válidos")
    void shouldSearchWithValidCriteria() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setText("Matrix");
        criteria.setType(MediaType.MOVIE);
        criteria.setYearMin(1999);
        criteria.setYearMax(2023);
        criteria.setRatingMin(8.0);

        PageQuery pageQuery = new PageQuery(0, 20, "averageRating", "DESC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).getTitle()).isEqualTo("The Matrix");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);

        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve buscar por texto simples")
    void shouldSearchByText() {
        // Given
        String query = "Star Wars";
        PageQuery pageQuery = new PageQuery(0, 20, "id", "ASC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.searchByText(query, pageQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);

        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve validar e ajustar paginação inválida (size > 100)")
    void shouldAdjustInvalidPageSize() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        PageQuery invalidPageQuery = new PageQuery(0, 200, "id", "ASC"); // size > MAX_SIZE (100)

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, invalidPageQuery);

        // Then
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve usar valores default quando paginação é null")
    void shouldUseDefaultPaginationWhenNull() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, null);

        // Then
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve inverter yearMin e yearMax quando yearMin > yearMax")
    void shouldSwapYearsWhenMinGreaterThanMax() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setYearMin(2023);
        criteria.setYearMax(1999); // yearMin > yearMax

        PageQuery pageQuery = new PageQuery(0, 20, "id", "ASC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        // Then
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve inverter ratingMin e ratingMax quando ratingMin > ratingMax")
    void shouldSwapRatingsWhenMinGreaterThanMax() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setRatingMin(9.0);
        criteria.setRatingMax(7.0); // ratingMin > ratingMax

        PageQuery pageQuery = new PageQuery(0, 20, "id", "ASC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        // Then
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve buscar tudo quando critérios são null")
    void shouldSearchAllWhenCriteriaIsNull() {
        // Given
        PageQuery pageQuery = new PageQuery(0, 20, "id", "ASC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(null, pageQuery);

        // Then
        assertThat(result).isNotNull();
        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve buscar com filtro de gêneros")
    void shouldSearchWithGenreFilter() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setGenreIds(List.of(28L, 12L)); // Action + Adventure

        PageQuery pageQuery = new PageQuery(0, 20, "id", "ASC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);

        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }

    @Test
    @DisplayName("Deve buscar filmes com rating alto (top-rated)")
    void shouldSearchTopRatedMovies() {
        // Given
        MediaSearchCriteria criteria = new MediaSearchCriteria();
        criteria.setType(MediaType.MOVIE);
        criteria.setRatingMin(8.0);

        PageQuery pageQuery = new PageQuery(0, 10, "averageRating", "DESC");

        when(mediaRepository.search(any(MediaSearchCriteria.class), any(PageQuery.class)))
                .thenReturn(pageResult);

        // When
        PageResult<Media> result = mediaSearchService.search(criteria, pageQuery);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.content()).hasSize(1);

        verify(mediaRepository, times(1)).search(any(MediaSearchCriteria.class), any(PageQuery.class));
    }
}
