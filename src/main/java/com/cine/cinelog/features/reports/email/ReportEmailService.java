package com.cine.cinelog.features.reports.email;

import com.cine.cinelog.features.reports.data.*;
import com.cine.cinelog.features.reports.query.*;
import com.cine.cinelog.features.users.repository.UserJpaRepository;
import com.cine.cinelog.features.users.persistence.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * High-level report email orchestrator.
 *
 * <p>
 * Combines query services with {@link EmailService} to produce and deliver
 * each type of report. All public methods are {@code @Async} and return
 * immediately.
 * </p>
 */
@Service
public class ReportEmailService {

    private static final Logger log = LoggerFactory.getLogger(ReportEmailService.class);

    private final EmailService emailService;
    private final WeeklyDigestQueryService weeklyDigest;
    private final TopRatedQueryService topRated;
    private final RecommendationsQueryService recommendations;
    private final TrendingQueryService trending;
    private final PlatformReportQueryService platform;
    private final TopActorsQueryService topActors;
    private final NewReleasesQueryService newReleases;
    private final GenreSpotlightQueryService genreSpotlight;
    private final UserJpaRepository userRepository;

    public ReportEmailService(EmailService emailService,
            WeeklyDigestQueryService weeklyDigest,
            TopRatedQueryService topRated,
            RecommendationsQueryService recommendations,
            TrendingQueryService trending,
            PlatformReportQueryService platform,
            TopActorsQueryService topActors,
            NewReleasesQueryService newReleases,
            GenreSpotlightQueryService genreSpotlight,
            UserJpaRepository userRepository) {
        this.emailService = emailService;
        this.weeklyDigest = weeklyDigest;
        this.topRated = topRated;
        this.recommendations = recommendations;
        this.trending = trending;
        this.platform = platform;
        this.topActors = topActors;
        this.newReleases = newReleases;
        this.genreSpotlight = genreSpotlight;
        this.userRepository = userRepository;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weekly Digest
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendWeeklyDigest(Long userId) {
        try {
            WeeklyDigestData data = weeklyDigest.buildForUser(userId);
            emailService.sendHtml(
                    data.getUserEmail(),
                    "📽️ Seu resumo semanal no CineLog",
                    "weekly-digest",
                    Map.of("data", data));
        } catch (Exception e) {
            log.error("Failed to send weekly digest to userId={}: {}", userId, e.getMessage(), e);
        }
    }

    @Async("reportTaskExecutor")
    public void sendWeeklyDigestToAll() {
        List<UserEntity> users = userRepository.findAll();
        log.info("Sending weekly digest to {} users", users.size());
        users.forEach(u -> sendWeeklyDigest(u.getId()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Rated
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendTopRated(String toEmail, int limit) {
        TopRatedData data = topRated.build(limit);
        emailService.sendHtml(
                toEmail,
                "⭐ Top " + limit + " mídias mais bem avaliadas no CineLog",
                "top-rated",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendRecommendations(Long userId) {
        try {
            RecommendationsData data = recommendations.buildForUser(userId);
            emailService.sendHtml(
                    data.getUserEmail(),
                    "🎬 Recomendações personalizadas para você no CineLog",
                    "recommendations",
                    Map.of("data", data));
        } catch (Exception e) {
            log.error("Failed to send recommendations to userId={}: {}", userId, e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Trending
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendTrending(String toEmail) {
        TrendingData data = trending.build();
        emailService.sendHtml(
                toEmail,
                "🔥 Em alta esta semana no CineLog",
                "trending",
                Map.of("data", data));
    }

    @Async("reportTaskExecutor")
    public void sendTrendingToAll() {
        List<UserEntity> users = userRepository.findAll();
        log.info("Sending trending report to {} users", users.size());
        users.forEach(u -> sendTrending(u.getEmail()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Platform Report (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendPlatformReport(String toEmail) {
        PlatformReportData data = platform.build();
        emailService.sendHtml(
                toEmail,
                "📊 Relatório da plataforma CineLog",
                "platform-report",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Actors (actors with highest-rated films)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendTopActors(String toEmail, int limit) {
        TopActorsData data = topActors.build(limit);
        emailService.sendHtml(
                toEmail,
                "🌟 Top " + limit + " atores com filmes mais bem avaliados no CineLog",
                "top-actors",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New Releases (recently added media)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendNewReleases(String toEmail, int days, int limit) {
        NewReleasesData data = newReleases.build(days, limit);
        emailService.sendHtml(
                toEmail,
                "🎬 " + data.getTotalNewMedia() + " novos títulos nos últimos " + days + " dias no CineLog",
                "new-releases",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genre Spotlight (deep dive into a genre)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendGenreSpotlight(String toEmail, String genreName) {
        GenreSpotlightData data = genreSpotlight.build(genreName);
        emailService.sendHtml(
                toEmail,
                "🎯 Gênero em destaque: " + data.getGenreName() + " — CineLog",
                "genre-spotlight",
                Map.of("data", data));
    }
}
