package com.cine.cinelog.features.media.integration.tmdb;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.tmdb.TmdbCredits;
import com.cine.cinelog.core.domain.model.tmdb.TmdbImageConfig;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbMediaSummary;
import com.cine.cinelog.core.domain.model.tmdb.TmdbPersonDetails;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewResult;
import com.cine.cinelog.core.domain.model.tmdb.TmdbReviewsPage;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbCreditsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbMovieSearchResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbPersonDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbReviewListResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbTvDetailsResponse;
import com.cine.cinelog.features.media.integration.tmdb.dto.TmdbTvSearchResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper responsavel por converter DTOs da TMDB API para modelos de dominio do CineLog.
 *
 * <p>Extraido do {@link TmdbClientAdapter} para atender ao Principio de Responsabilidade
 * Unica (SRP): o adapter gerencia chamadas HTTP e resiliencia, este mapper gerencia
 * a transformacao de dados.</p>
 */
@Slf4j
@Component
public class TmdbResponseMapper {

    private static final String TMDB_IMAGE_FALLBACK_BASE = "https://image.tmdb.org/t/p/";

    // ── Media Details ────────────────────────────────────────────────────────

    public TmdbMediaDetails mapMovieDetailsToDomain(TmdbMovieDetailsResponse dto, TmdbImageConfig imageConfig) {
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

    public TmdbMediaDetails mapTvDetailsToDomain(TmdbTvDetailsResponse dto, TmdbImageConfig imageConfig) {
        return TmdbMediaDetails.builder()
                .tmdbId(dto.getId() != null ? dto.getId().longValue() : null)
                .type(MediaType.SERIES)
                .title(dto.getName())
                .originalTitle(dto.getOriginalName())
                .originalLanguage(dto.getOriginalLanguage())
                .overview(dto.getOverview())
                .releaseYear(parseYear(dto.getFirstAirDate()))
                .voteAverage(dto.getVoteAverage())
                .runtimeMinutes(null)
                .posterUrl(buildPosterUrl(imageConfig, dto.getPosterPath()))
                .backdropUrl(buildBackdropUrl(imageConfig, dto.getBackdropPath()))
                .genres(dto.getGenres() == null
                        ? Collections.emptyList()
                        : dto.getGenres().stream()
                                .map(TmdbMovieDetailsResponse.Genre::getName)
                                .collect(Collectors.toList()))
                .numberOfSeasons(dto.getNumberOfSeasons())
                .build();
    }

    // ── Search Summaries ─────────────────────────────────────────────────────

    public TmdbMediaSummary mapMovieSearchResultToSummary(TmdbMovieSearchResponse.Result r, TmdbImageConfig imageConfig) {
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

    public TmdbMediaSummary mapTvSearchResultToSummary(TmdbTvSearchResponse.Result r, TmdbImageConfig imageConfig) {
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

    // ── Credits ──────────────────────────────────────────────────────────────

    public TmdbCredits mapCreditsToDomain(TmdbCreditsResponse dto, TmdbImageConfig imageConfig) {
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

    // ── Person Details ───────────────────────────────────────────────────────

    public TmdbPersonDetails mapPersonDetailsToDomain(TmdbPersonDetailsResponse resp, TmdbImageConfig imageConfig) {
        TmdbPersonDetails details = new TmdbPersonDetails();
        details.setTmdbPersonId(resp.getId());
        details.setName(resp.getName());
        details.setBiography(resp.getBiography());
        details.setBirthday(parseDate(resp.getBirthday()));
        details.setDeathday(parseDate(resp.getDeathday()));
        details.setGender(resp.getGender());
        details.setHomepage(resp.getHomepage());
        details.setImdbId(resp.getImdbId());
        details.setKnownForDepartment(resp.getKnownForDepartment());
        details.setPlaceOfBirth(resp.getPlaceOfBirth());
        details.setPopularity(resp.getPopularity());
        details.setProfilePath(buildProfileUrl(imageConfig, resp.getProfilePath()));
        return details;
    }

    // ── Reviews ──────────────────────────────────────────────────────────────

    public TmdbReviewsPage mapReviewsPage(TmdbReviewListResponse resp, Long tmdbMediaId) {
        TmdbReviewsPage page = new TmdbReviewsPage();
        page.setTmdbMediaId(tmdbMediaId);
        if (resp == null) {
            page.setResults(Collections.emptyList());
            return page;
        }
        page.setPage(resp.getPage());
        page.setTotalPages(resp.getTotalPages());
        page.setTotalResults(resp.getTotalResults());
        List<TmdbReviewResult> results = new ArrayList<>();
        if (resp.getResults() != null) {
            for (TmdbReviewListResponse.ReviewItem item : resp.getResults()) {
                TmdbReviewResult r = new TmdbReviewResult();
                r.setId(item.getId());
                r.setAuthor(item.getAuthor());
                r.setContent(item.getContent());
                r.setUrl(item.getUrl());
                r.setCreatedAt(parseOffsetDateTime(item.getCreatedAt()));
                r.setUpdatedAt(parseOffsetDateTime(item.getUpdatedAt()));
                TmdbReviewListResponse.AuthorDetails ad = item.getAuthorDetails();
                if (ad != null) {
                    r.setAuthorUsername(ad.getUsername());
                    r.setAuthorAvatarPath(ad.getAvatarPath());
                    r.setAuthorRating(ad.getRating());
                }
                results.add(r);
            }
        }
        page.setResults(results);
        return page;
    }

    public TmdbReviewsPage emptyReviewsPage(Long tmdbMediaId) {
        TmdbReviewsPage p = new TmdbReviewsPage();
        p.setTmdbMediaId(tmdbMediaId);
        p.setResults(Collections.emptyList());
        return p;
    }

    // ── Date/URL helpers ─────────────────────────────────────────────────────

    Integer parseYear(String dateStr) {
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

    LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Could not parse TMDb date '{}' as LocalDate", dateStr);
            return null;
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            return null;
        }
    }

    String buildPosterUrl(TmdbImageConfig config, String path) {
        return buildImageUrl(config, path, "w500");
    }

    String buildBackdropUrl(TmdbImageConfig config, String path) {
        return buildImageUrl(config, path, "w780");
    }

    String buildProfileUrl(TmdbImageConfig config, String path) {
        return buildImageUrl(config, path, "w185");
    }

    private String buildImageUrl(TmdbImageConfig config, String path, String size) {
        if (path == null || path.isBlank()) {
            return null;
        }
        if (config == null || config.getSecureBaseUrl() == null) {
            return TMDB_IMAGE_FALLBACK_BASE + size + path;
        }
        return switch (size) {
            case "w500" -> config.buildPosterUrl(size, path);
            case "w780" -> config.buildBackdropUrl(size, path);
            case "w185" -> config.buildProfileUrl(size, path);
            default -> config.getSecureBaseUrl() + size + path;
        };
    }
}
