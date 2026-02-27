package com.cine.cinelog.features.media.integration.tmdb;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.cine.cinelog.core.application.ports.out.TmdbClientPort;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;
import com.cine.cinelog.core.domain.model.tmdb.TmdbDiscoverQuery;
import com.cine.cinelog.core.domain.model.tmdb.TmdbGenre;
import com.cine.cinelog.core.domain.model.tmdb.TmdbImageConfig;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbSearchResult;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbConfigurationResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbCreditsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbGenreListResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieSearchResponse;
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

    /**
     * Cache simples em memória da configuração de imagens do TMDb.
     */
    private volatile TmdbImageConfig cachedImageConfig;

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

            return Optional.of(mapMovieDetailsToDomain(response));
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

            return Optional.of(mapTvDetailsToDomain(response));
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
                .map(r -> mapMovieSearchResultToSummary(r))
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
                .map(r -> mapTvSearchResultToSummary(r))
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
                .map(this::mapMovieSearchResultToSummary)
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
                .map(this::mapTvSearchResultToSummary)
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

        return Optional.of(mapCreditsToDomain(response));
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

        return Optional.of(mapCreditsToDomain(response));
    }

    // ========================================================================
    // Configuração de imagens
    // ========================================================================

    @Override
    @Retry(name = TMDB_INSTANCE)
    @CircuitBreaker(name = TMDB_INSTANCE, fallbackMethod = "fallbackFetchImageConfig")
    public TmdbImageConfig fetchImageConfig() {
        // usa cache simples em memória
        TmdbImageConfig localCache = cachedImageConfig;
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
            cachedImageConfig = empty;
            return empty;
        }

        TmdbConfigurationResponse.Images images = response.getImages();

        TmdbImageConfig config = TmdbImageConfig.builder()
                .baseUrl(images.getBaseUrl())
                .secureBaseUrl(images.getSecureBaseUrl())
                .posterSizes(images.getPosterSizes())
                .backdropSizes(images.getBackdropSizes())
                .profileSizes(images.getProfileSizes())
                .build();

        cachedImageConfig = config;
        return config;
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
        TmdbImageConfig localCache = cachedImageConfig;
        if (localCache != null) {
            return localCache;
        }
        return TmdbImageConfig.builder().build();
    }

    // ========================================================================
    // Mapping helpers
    // ========================================================================

    private TmdbMediaDetails mapMovieDetailsToDomain(TmdbMovieDetailsResponse dto) {
        TmdbImageConfig imageConfig = fetchImageConfig();

        return TmdbMediaDetails.builder()
                .tmdbId(dto.getId() != null ? dto.getId().longValue() : null)
                .type(MediaType.MOVIE)
                .title(dto.getTitle())
                .originalTitle(dto.getOriginalTitle())
                .originalLanguage(dto.getOriginalLanguage())
                .overview(dto.getOverview())
                .releaseYear(parseYear(dto.getReleaseDate()))
                .voteAverage(dto.getVoteAverage())
                .runtimeMinutes(dto.getRuntime())
                .posterUrl(buildPosterUrl(imageConfig, dto.getPosterPath()))
                .backdropUrl(buildBackdropUrl(imageConfig, dto.getBackdropPath()))
                .genres(dto.getGenres() == null
                        ? Collections.emptyList()
                        : dto.getGenres().stream()
                                .map(TmdbMovieDetailsResponse.Genre::getName)
                                .collect(Collectors.toList()))
                .build();
    }

    private TmdbMediaDetails mapTvDetailsToDomain(TmdbTvDetailsResponse dto) {
        TmdbImageConfig imageConfig = fetchImageConfig();

        return TmdbMediaDetails.builder()
                .tmdbId(dto.getId() != null ? dto.getId().longValue() : null)
                .type(MediaType.SERIES)
                .title(dto.getName())
                .originalTitle(dto.getOriginalName())
                .originalLanguage(dto.getOriginalLanguage())
                .overview(dto.getOverview())
                .releaseYear(parseYear(dto.getFirstAirDate()))
                .voteAverage(dto.getVoteAverage())
                .runtimeMinutes(null) // poderia usar avg episode runtime em outra chamada
                .posterUrl(buildPosterUrl(imageConfig, dto.getPosterPath()))
                .backdropUrl(buildBackdropUrl(imageConfig, dto.getBackdropPath()))
                .genres(dto.getGenres() == null
                        ? Collections.emptyList()
                        : dto.getGenres().stream()
                                .map(TmdbMovieDetailsResponse.Genre::getName)
                                .collect(Collectors.toList()))
                .build();
    }

    private TmdbMediaSummary mapMovieSearchResultToSummary(TmdbMovieSearchResponse.Result r) {
        TmdbImageConfig imageConfig = fetchImageConfig();

        return TmdbMediaSummary.builder()
                .tmdbId(r.getId() != null ? r.getId().longValue() : null)
                .type(MediaType.MOVIE)
                .title(r.getTitle())
                .releaseYear(parseYear(r.getReleaseDate()))
                .overview(r.getOverview())
                .voteAverage(r.getVoteAverage())
                .posterUrl(buildPosterUrl(imageConfig, r.getPosterPath()))
                .build();
    }

    private TmdbMediaSummary mapTvSearchResultToSummary(TmdbTvSearchResponse.Result r) {
        TmdbImageConfig imageConfig = fetchImageConfig();

        return TmdbMediaSummary.builder()
                .tmdbId(r.getId() != null ? r.getId().longValue() : null)
                .type(MediaType.SERIES)
                .title(r.getName())
                .releaseYear(parseYear(r.getFirstAirDate()))
                .overview(r.getOverview())
                .voteAverage(r.getVoteAverage())
                .posterUrl(buildPosterUrl(imageConfig, r.getPosterPath()))
                .build();
    }

    private TmdbCredits mapCreditsToDomain(TmdbCreditsResponse dto) {
        TmdbImageConfig imageConfig = fetchImageConfig();

        List<TmdbCredits.CastMember> cast = dto.getCast() == null
                ? Collections.emptyList()
                : dto.getCast().stream()
                        .map(c -> TmdbCredits.CastMember.builder()
                                .tmdbPersonId(c.getId() != null ? c.getId().longValue() : null)
                                .name(c.getName())
                                .character(c.getCharacter())
                                .castOrder(c.getCastOrder())
                                .profileUrl(buildProfileUrl(imageConfig, c.getProfilePath()))
                                .build())
                        .collect(Collectors.toList());

        List<TmdbCredits.CrewMember> crew = dto.getCrew() == null
                ? Collections.emptyList()
                : dto.getCrew().stream()
                        .map(c -> TmdbCredits.CrewMember.builder()
                                .tmdbPersonId(c.getId() != null ? c.getId().longValue() : null)
                                .name(c.getName())
                                .job(c.getJob())
                                .department(c.getDepartment())
                                .profileUrl(buildProfileUrl(imageConfig, c.getProfilePath()))
                                .build())
                        .collect(Collectors.toList());

        return TmdbCredits.builder()
                .tmdbId(dto.getId() != null ? dto.getId().longValue() : null)
                .cast(cast)
                .crew(crew)
                .build();
    }

    private Integer parseYear(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr).getYear();
        } catch (Exception e) {
            log.warn("Could not parse TMDb date '{}' as LocalDate", dateStr);
            try {
                return Integer.parseInt(dateStr.substring(0, 4));
            } catch (Exception ignore) {
                return null;
            }
        }
    }

    private String buildPosterUrl(TmdbImageConfig config, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        // escolha de tamanho padrão; poderia vir de config
        String size = "w500";
        if (config.getSecureBaseUrl() == null) {
            // fallback para domínio estático padrão do TMDb
            return "https://image.tmdb.org/t/p/" + size + path;
        }
        return config.buildPosterUrl(size, path);
    }

    private String buildBackdropUrl(TmdbImageConfig config, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String size = "w780";
        if (config.getSecureBaseUrl() == null) {
            return "https://image.tmdb.org/t/p/" + size + path;
        }
        return config.buildBackdropUrl(size, path);
    }

    private String buildProfileUrl(TmdbImageConfig config, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String size = "w185";
        if (config.getSecureBaseUrl() == null) {
            return "https://image.tmdb.org/t/p/" + size + path;
        }
        return config.buildProfileUrl(size, path);
    }
}
