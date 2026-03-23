package com.cine.cinelog.features.media.integration.tmdb;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;
import com.cine.cinelog.core.domain.model.tmdb.TmdbDiscoverQuery;
import com.cine.cinelog.core.domain.model.tmdb.TmdbEpisodeDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbGenre;
import com.cine.cinelog.core.domain.model.tmdb.TmdbImageConfig;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbPersonDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewsPage;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSearchResult;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSeasonDetails;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbConfigurationResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbCreditsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbGenreListResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieSearchResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbPersonDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbReviewListResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbSeasonDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbTvDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbTvSearchResponse;
import com.cine.cinelog.shared.config.tmdb.TmdbProperties;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheConfig;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

/**
 * Implementação da porta TmdbClientPort usando WebClient + Resilience4j.
 *
 * <p>
 * Esta classe está na camada de infraestrutura e contém todo o mapeamento
 * entre o JSON do TMDb (DTOs) e os modelos de domínio do CineLog.
 */
@CacheConfig(cacheNames = "tmdb")
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClientAdapter implements TmdbClientPort {

    private static final String TMDB_INSTANCE = "tmdb";

    private final WebClient tmdbWebClient;
    private final TmdbProperties properties;
    private final TmdbResponseMapper mapper;

    /**
     * Cache simples em memoria da configuracao de imagens do TMDb.
     * AtomicReference para evitar double-init em cenario multi-thread.
     */
    private final java.util.concurrent.atomic.AtomicReference<TmdbImageConfig> cachedImageConfig =
            new java.util.concurrent.atomic.AtomicReference<>();

    // ========================================================================
    // Detalhes de mídia
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchByTmdbId")
    @Measured("cinelog.integration.tmdb.fetch_by_id")
    @AlertIfSlow(thresholdMs = 3000, metricName = "cinelog.slow_tmdb_fetch")
    public Optional<TmdbMediaDetails> fetchByTmdbId(Long tmdbId) {
        if (tmdbId == null) {
            return Optional.empty();
        }

        // 1) tenta como filme
        Optional<TmdbMediaDetails> movie = fetchMovie(tmdbId);
        if (movie.isPresent()) {
            return movie;
        }

        // 2) tenta como série
        return fetchTv(tmdbId);
    }

    @SuppressWarnings("unused")
    private Optional<TmdbMediaDetails> fallbackFetchByTmdbId(Long tmdbId, Throwable ex) {
        log.warn("Fallback fetchByTmdbId(tmdbId={}) due to {}", tmdbId, ex.toString());
        return Optional.empty();
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovie")
    public Optional<TmdbMediaDetails> fetchMovie(Long tmdbId) {
        if (tmdbId == null) {
            return Optional.empty();
        }

        try {
            TmdbMovieDetailsResponse response = tmdbWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/movie/{id}")
                            .queryParam("language", properties.getLanguage())
                            .build(tmdbId))
                    .retrieve()
                    .bodyToMono(TmdbMovieDetailsResponse.class)
                    .block();

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(mapper.mapMovieDetailsToDomain(response, fetchImageConfig()));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                log.info("TMDb movie not found for id={}", tmdbId);
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTv")
    public Optional<TmdbMediaDetails> fetchTv(Long tmdbId) {
        if (tmdbId == null) {
            return Optional.empty();
        }

        try {
            TmdbTvDetailsResponse response = tmdbWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tv/{id}")
                            .queryParam("language", properties.getLanguage())
                            .build(tmdbId))
                    .retrieve()
                    .bodyToMono(TmdbTvDetailsResponse.class)
                    .block();

            if (response == null) {
                return Optional.empty();
            }

            return Optional.of(mapper.mapTvDetailsToDomain(response, fetchImageConfig()));
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                log.info("TMDb tv show not found for id={}", tmdbId);
                return Optional.empty();
            }
            throw ex;
        }
    }

    // ========================================================================
    // Busca textual (search)
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackSearchByTitleAndYear")
    public Optional<TmdbMediaDetails> searchByTitleAndYear(String title, Integer year) {
        if (title == null || title.isBlank()) {
            return Optional.empty();
        }

        // 1) tenta filme
        TmdbSearchResult<TmdbMediaSummary> movieSearch = searchMovies(title, year, 1);
        if (!movieSearch.isEmpty()) {
            TmdbMediaSummary first = movieSearch.getResults().get(0);
            return fetchMovie(first.getTmdbId());
        }

        // 2) tenta série
        TmdbSearchResult<TmdbMediaSummary> tvSearch = searchTvShows(title, year, 1);
        if (!tvSearch.isEmpty()) {
            TmdbMediaSummary first = tvSearch.getResults().get(0);
            return fetchTv(first.getTmdbId());
        }

        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<TmdbMediaDetails> fallbackSearchByTitleAndYear(String title, Integer year, Throwable ex) {
        log.warn("Fallback searchByTitleAndYear(title='{}', year={}) due to {}", title, year, ex.toString());
        return Optional.empty();
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackSearchMovies")
    @Bulkhead(name = TMDB_INSTANCE)
    public TmdbSearchResult<TmdbMediaSummary> searchMovies(String query, Integer year, int page) {
        if (query == null || query.isBlank()) {
            return TmdbSearchResult.<TmdbMediaSummary>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .build();
        }

        TmdbMovieSearchResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/search/movie")
                            .queryParam("query", query)
                            .queryParam("language", properties.getLanguage())
                            .queryParam("region", properties.getRegion())
                            .queryParam("page", page <= 0 ? 1 : page);
                    if (year != null) {
                        builder.queryParam("year", year);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(TmdbMovieSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return TmdbSearchResult.<TmdbMediaSummary>builder()
                    .page(page)
                    .totalPages(0)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .build();
        }

        List<TmdbMediaSummary> items = response.getResults().stream()
                .map(r -> mapper.mapMovieSearchResultToSummary(r, fetchImageConfig()))
                .collect(Collectors.toList());

        return TmdbSearchResult.<TmdbMediaSummary>builder()
                .page(response.getPage())
                .totalPages(response.getTotalPages())
                .totalResults(response.getTotalResults())
                .results(items)
                .build();
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackSearchTvShows")
    @Bulkhead(name = TMDB_INSTANCE)
    public TmdbSearchResult<TmdbMediaSummary> searchTvShows(String query, Integer year, int page) {
        if (query == null || query.isBlank()) {
            return TmdbSearchResult.<TmdbMediaSummary>builder()
                    .page(1)
                    .totalPages(1)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .build();
        }

        TmdbTvSearchResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/search/tv")
                            .queryParam("query", query)
                            .queryParam("language", properties.getLanguage())
                            .queryParam("region", properties.getRegion())
                            .queryParam("page", page <= 0 ? 1 : page);
                    if (year != null) {
                        builder.queryParam("first_air_date_year", year);
                    }
                    return builder.build();
                })
                .retrieve()
                .bodyToMono(TmdbTvSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return TmdbSearchResult.<TmdbMediaSummary>builder()
                    .page(page)
                    .totalPages(0)
                    .totalResults(0)
                    .results(Collections.emptyList())
                    .build();
        }

        List<TmdbMediaSummary> items = response.getResults().stream()
                .map(r -> mapper.mapTvSearchResultToSummary(r, fetchImageConfig()))
                .collect(Collectors.toList());

        return TmdbSearchResult.<TmdbMediaSummary>builder()
                .page(response.getPage())
                .totalPages(response.getTotalPages())
                .totalResults(response.getTotalResults())
                .results(items)
                .build();
    }

    // ========================================================================
    // Discover
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackDiscoverMovies")
    @Bulkhead(name = TMDB_INSTANCE)
    public TmdbSearchResult<TmdbMediaSummary> discoverMovies(TmdbDiscoverQuery query) {
        int page = query.getPage() == null || query.getPage() <= 0 ? 1 : query.getPage();

        TmdbMovieSearchResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/discover/movie")
                            .queryParam("language", properties.getLanguage())
                            .queryParam("region", properties.getRegion())
                            .queryParam("page", page);

                    if (query.getWithGenres() != null && !query.getWithGenres().isBlank()) {
                        builder.queryParam("with_genres", query.getWithGenres());
                    }
                    if (query.getSortBy() != null && !query.getSortBy().isBlank()) {
                        builder.queryParam("sort_by", query.getSortBy());
                    }
                    if (query.getYear() != null) {
                        builder.queryParam("primary_release_year", query.getYear());
                    }

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(TmdbMovieSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return emptySearchResult(page);
        }

        List<TmdbMediaSummary> items = response.getResults().stream()
                .map(r -> mapper.mapMovieSearchResultToSummary(r, fetchImageConfig()))
                .collect(Collectors.toList());

        return TmdbSearchResult.<TmdbMediaSummary>builder()
                .page(response.getPage())
                .totalPages(response.getTotalPages())
                .totalResults(response.getTotalResults())
                .results(items)
                .build();
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackDiscoverTvShows")
    @Bulkhead(name = TMDB_INSTANCE)
    public TmdbSearchResult<TmdbMediaSummary> discoverTvShows(TmdbDiscoverQuery query) {
        int page = query.getPage() == null || query.getPage() <= 0 ? 1 : query.getPage();

        TmdbTvSearchResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/discover/tv")
                            .queryParam("language", properties.getLanguage())
                            .queryParam("region", properties.getRegion())
                            .queryParam("page", page);

                    if (query.getWithGenres() != null && !query.getWithGenres().isBlank()) {
                        builder.queryParam("with_genres", query.getWithGenres());
                    }
                    if (query.getSortBy() != null && !query.getSortBy().isBlank()) {
                        builder.queryParam("sort_by", query.getSortBy());
                    }
                    if (query.getYear() != null) {
                        builder.queryParam("first_air_date_year", query.getYear());
                    }

                    return builder.build();
                })
                .retrieve()
                .bodyToMono(TmdbTvSearchResponse.class)
                .block();

        if (response == null || response.getResults() == null) {
            return emptySearchResult(page);
        }

        List<TmdbMediaSummary> items = response.getResults().stream()
                .map(r -> mapper.mapTvSearchResultToSummary(r, fetchImageConfig()))
                .collect(Collectors.toList());

        return TmdbSearchResult.<TmdbMediaSummary>builder()
                .page(response.getPage())
                .totalPages(response.getTotalPages())
                .totalResults(response.getTotalResults())
                .results(items)
                .build();
    }

    private TmdbSearchResult<TmdbMediaSummary> emptySearchResult(int page) {
        return TmdbSearchResult.<TmdbMediaSummary>builder()
                .page(page)
                .totalPages(0)
                .totalResults(0)
                .results(Collections.emptyList())
                .build();
    }

    // ========================================================================
    // Gêneros
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovieGenres")
    public List<TmdbGenre> fetchMovieGenres() {
        TmdbGenreListResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/movie/list")
                        .queryParam("language", properties.getLanguage())
                        .build())
                .retrieve()
                .bodyToMono(TmdbGenreListResponse.class)
                .block();

        if (response == null || response.getGenres() == null) {
            return Collections.emptyList();
        }

        return response.getGenres().stream()
                .map(g -> TmdbGenre.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .type(MediaType.MOVIE)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvGenres")
    public List<TmdbGenre> fetchTvGenres() {
        TmdbGenreListResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/genre/tv/list")
                        .queryParam("language", properties.getLanguage())
                        .build())
                .retrieve()
                .bodyToMono(TmdbGenreListResponse.class)
                .block();

        if (response == null || response.getGenres() == null) {
            return Collections.emptyList();
        }

        return response.getGenres().stream()
                .map(g -> TmdbGenre.builder()
                        .id(g.getId())
                        .name(g.getName())
                        .type(MediaType.SERIES)
                        .build())
                .collect(Collectors.toList());
    }

    // ========================================================================
    // Créditos
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovieCredits")
    public Optional<TmdbCredits> fetchMovieCredits(Long movieId) {
        if (movieId == null) {
            return Optional.empty();
        }

        TmdbCreditsResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}/credits")
                        .queryParam("language", properties.getLanguage())
                        .build(movieId))
                .retrieve()
                .bodyToMono(TmdbCreditsResponse.class)
                .block();

        if (response == null) {
            return Optional.empty();
        }

        return Optional.of(mapper.mapCreditsToDomain(response, fetchImageConfig()));
    }

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvCredits")
    public Optional<TmdbCredits> fetchTvCredits(Long tvId) {
        if (tvId == null) {
            return Optional.empty();
        }

        TmdbCreditsResponse response = tmdbWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/credits")
                        .queryParam("language", properties.getLanguage())
                        .build(tvId))
                .retrieve()
                .bodyToMono(TmdbCreditsResponse.class)
                .block();

        if (response == null) {
            return Optional.empty();
        }

        return Optional.of(mapper.mapCreditsToDomain(response, fetchImageConfig()));
    }

    // ========================================================================
    // Configuração de imagens
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchImageConfig")
    public TmdbImageConfig fetchImageConfig() {
        TmdbImageConfig localCache = cachedImageConfig.get();
        if (localCache != null) {
            return localCache;
        }

        TmdbConfigurationResponse response = tmdbWebClient.get()
                .uri("/configuration")
                .retrieve()
                .bodyToMono(TmdbConfigurationResponse.class)
                .block();

        if (response == null || response.getImages() == null) {
            log.warn("TMDb configuration returned null images");
            TmdbImageConfig empty = TmdbImageConfig.builder().build();
            cachedImageConfig.compareAndSet(null, empty);
            return cachedImageConfig.get();
        }

        TmdbConfigurationResponse.Images images = response.getImages();

        TmdbImageConfig config = TmdbImageConfig.builder()
                .baseUrl(images.getBaseUrl())
                .secureBaseUrl(images.getSecureBaseUrl())
                .posterSizes(images.getPosterSizes())
                .backdropSizes(images.getBackdropSizes())
                .profileSizes(images.getProfileSizes())
                .build();

        cachedImageConfig.compareAndSet(null, config);
        return cachedImageConfig.get();
    }

    // ========================================================================
    // Temporadas
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvSeason")
    @Bulkhead(name = TMDB_INSTANCE)
    public Optional<TmdbSeasonDetails> fetchTvSeason(Long tvId, int seasonNumber) {
        if (tvId == null) {
            return Optional.empty();
        }

        try {
            TmdbSeasonDetailsResponse response = tmdbWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tv/{id}/season/{seasonNumber}")
                            .queryParam("language", properties.getLanguage())
                            .build(tvId, seasonNumber))
                    .retrieve()
                    .bodyToMono(TmdbSeasonDetailsResponse.class)
                    .block();

            if (response == null) {
                return Optional.empty();
            }

            List<TmdbEpisodeDetails> episodes = Optional.ofNullable(response.getEpisodes())
                    .orElse(Collections.emptyList())
                    .stream()
                    .map(ep -> TmdbEpisodeDetails.builder()
                            .episodeNumber(ep.getEpisodeNumber())
                            .name(ep.getName())
                            .airDate(mapper.parseDate(ep.getAirDate()))
                            .build())
                    .collect(Collectors.toList());

            return Optional.of(TmdbSeasonDetails.builder()
                    .seasonNumber(response.getSeasonNumber())
                    .name(response.getName())
                    .airDate(mapper.parseDate(response.getAirDate()))
                    .episodes(episodes)
                    .build());
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode().equals(HttpStatusCode.valueOf(404))) {
                log.info("TMDb season not found for tvId={} season={}", tvId, seasonNumber);
                return Optional.empty();
            }
            throw ex;
        }
    }

    // ========================================================================
    // A10:2025 — Fallback methods (Resilience4j)
    // ========================================================================

    @SuppressWarnings("unused")
    private Optional<TmdbMediaDetails> fallbackFetchMovie(Long tmdbId, Throwable ex) {
        log.warn("Fallback fetchMovie(tmdbId={}) due to {}", tmdbId, ex.toString());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<TmdbMediaDetails> fallbackFetchTv(Long tmdbId, Throwable ex) {
        log.warn("Fallback fetchTv(tmdbId={}) due to {}", tmdbId, ex.toString());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private TmdbSearchResult<TmdbMediaSummary> fallbackSearchMovies(String query, Integer year, int page,
            Throwable ex) {
        log.warn("Fallback searchMovies(query='{}', year={}, page={}) due to {}", query, year, page, ex.toString());
        return emptySearchResult(page);
    }

    @SuppressWarnings("unused")
    private TmdbSearchResult<TmdbMediaSummary> fallbackSearchTvShows(String query, Integer year, int page,
            Throwable ex) {
        log.warn("Fallback searchTvShows(query='{}', year={}, page={}) due to {}", query, year, page, ex.toString());
        return emptySearchResult(page);
    }

    @SuppressWarnings("unused")
    private TmdbSearchResult<TmdbMediaSummary> fallbackDiscoverMovies(TmdbDiscoverQuery query, Throwable ex) {
        log.warn("Fallback discoverMovies due to {}", ex.toString());
        return emptySearchResult(query.getPage() != null ? query.getPage() : 1);
    }

    @SuppressWarnings("unused")
    private TmdbSearchResult<TmdbMediaSummary> fallbackDiscoverTvShows(TmdbDiscoverQuery query, Throwable ex) {
        log.warn("Fallback discoverTvShows due to {}", ex.toString());
        return emptySearchResult(query.getPage() != null ? query.getPage() : 1);
    }

    @SuppressWarnings("unused")
    private List<TmdbGenre> fallbackFetchMovieGenres(Throwable ex) {
        log.warn("Fallback fetchMovieGenres due to {}", ex.toString());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private List<TmdbGenre> fallbackFetchTvGenres(Throwable ex) {
        log.warn("Fallback fetchTvGenres due to {}", ex.toString());
        return Collections.emptyList();
    }

    @SuppressWarnings("unused")
    private Optional<TmdbCredits> fallbackFetchMovieCredits(Long movieId, Throwable ex) {
        log.warn("Fallback fetchMovieCredits(movieId={}) due to {}", movieId, ex.toString());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private Optional<TmdbCredits> fallbackFetchTvCredits(Long tvId, Throwable ex) {
        log.warn("Fallback fetchTvCredits(tvId={}) due to {}", tvId, ex.toString());
        return Optional.empty();
    }

    @SuppressWarnings("unused")
    private TmdbImageConfig fallbackFetchImageConfig(Throwable ex) {
        log.warn("Fallback fetchImageConfig due to {}", ex.toString());
        TmdbImageConfig localCache = cachedImageConfig.get();
        if (localCache != null) {
            return localCache;
        }
        return TmdbImageConfig.builder().build();
    }

    @SuppressWarnings("unused")
    private Optional<TmdbSeasonDetails> fallbackFetchTvSeason(Long tvId, int seasonNumber, Throwable ex) {
        log.warn("Fallback fetchTvSeason(tvId={}, season={}) due to {}", tvId, seasonNumber, ex.toString());
        return Optional.empty();
    }

    // ================== Reviews ==================

    @Override
    @Retry(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovieReviews")
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovieReviews")
    @Bulkhead(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchMovieReviews")
    public TmdbReviewsPage fetchMovieReviews(Long movieTmdbId, int page) {
        log.debug("Fetching movie reviews tmdbId={} page={}", movieTmdbId, page);
        TmdbReviewListResponse resp = tmdbWebClient.get()
                .uri(u -> u.path("/movie/{id}/reviews")
                        .queryParam("language", properties.getLanguage())
                        .queryParam("page", page)
                        .build(movieTmdbId))
                .retrieve()
                .bodyToMono(TmdbReviewListResponse.class)
                .block();
        return mapper.mapReviewsPage(resp, movieTmdbId);
    }

    public TmdbReviewsPage fallbackFetchMovieReviews(Long movieTmdbId, int page, Throwable t) {
        log.warn("Fallback fetchMovieReviews tmdbId={} page={}: {}", movieTmdbId, page, t.getMessage());
        return mapper.emptyReviewsPage(movieTmdbId);
    }

    @Override
    @Retry(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvReviews")
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvReviews")
    @Bulkhead(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchTvReviews")
    public TmdbReviewsPage fetchTvReviews(Long tvTmdbId, int page) {
        log.debug("Fetching TV reviews tmdbId={} page={}", tvTmdbId, page);
        TmdbReviewListResponse resp = tmdbWebClient.get()
                .uri(u -> u.path("/tv/{id}/reviews")
                        .queryParam("language", properties.getLanguage())
                        .queryParam("page", page)
                        .build(tvTmdbId))
                .retrieve()
                .bodyToMono(TmdbReviewListResponse.class)
                .block();
        return mapper.mapReviewsPage(resp, tvTmdbId);
    }

    public TmdbReviewsPage fallbackFetchTvReviews(Long tvTmdbId, int page, Throwable t) {
        log.warn("Fallback fetchTvReviews tmdbId={} page={}: {}", tvTmdbId, page, t.getMessage());
        return mapper.emptyReviewsPage(tvTmdbId);
    }

    // ================== Detalhes de Pessoa ==================

    @Override
    @Retry(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchPersonDetails")
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchPersonDetails")
    @Bulkhead(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchPersonDetails")
    public Optional<TmdbPersonDetails> fetchPersonDetails(Long tmdbPersonId) {
        log.debug("Fetching person details tmdbPersonId={}", tmdbPersonId);
        try {
            TmdbPersonDetailsResponse resp = tmdbWebClient.get()
                    .uri(u -> u.path("/person/{id}")
                            .queryParam("language", properties.getLanguage())
                            .build(tmdbPersonId))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, r -> {
                        log.warn("Person not found tmdbPersonId={} status={}", tmdbPersonId, r.statusCode());
                        return r.createException();
                    })
                    .bodyToMono(TmdbPersonDetailsResponse.class)
                    .block();
            if (resp == null)
                return Optional.empty();
            return Optional.of(mapper.mapPersonDetailsToDomain(resp, fetchImageConfig()));
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Person not found on TMDB tmdbPersonId={}", tmdbPersonId);
            return Optional.empty();
        }
    }

    public Optional<TmdbPersonDetails> fallbackFetchPersonDetails(Long tmdbPersonId, Throwable t) {
        log.warn("Fallback fetchPersonDetails tmdbPersonId={}: {}", tmdbPersonId, t.getMessage());
        return Optional.empty();
    }
}
