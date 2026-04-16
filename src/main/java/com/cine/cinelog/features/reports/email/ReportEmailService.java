package com.cine.cinelog.features.reports.email;

import com.cine.cinelog.features.reports.config.ReportProperties;
import com.cine.cinelog.features.reports.data.*;
import com.cine.cinelog.features.reports.pdf.GotenbergPdfService;
import com.cine.cinelog.features.reports.pdf.PdfOptions;
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
    private final GotenbergPdfService pdfService;
    private final ReportProperties reportProps;
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
            GotenbergPdfService pdfService,
            ReportProperties reportProps,
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
        this.pdfService = pdfService;
        this.reportProps = reportProps;
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
    // Helper — optionally attach PDF
    // ─────────────────────────────────────────────────────────────────────────

    private boolean shouldAttachPdf() {
        return reportProps.getPdf().isEnabled() && reportProps.getPdf().isAttachToEmail();
    }

    private byte[] tryGeneratePdf(String templateName, Map<String, Object> data) {
        try {
            return pdfService.generate(PdfOptions.a4(templateName), data);
        } catch (Exception e) {
            log.warn("PDF generation failed for {}, sending email without attachment: {}",
                    templateName, e.getMessage());
            return new byte[0];
        }
    }

    private void sendWithOptionalPdf(String to, String subject, String emailTemplate,
            String pdfTemplate, Map<String, Object> variables) {
        if (shouldAttachPdf()) {
            byte[] pdf = tryGeneratePdf(pdfTemplate, variables);
            emailService.sendHtmlWithAttachment(to, subject, emailTemplate, variables,
                    pdf, pdfTemplate + ".pdf");
        } else {
            emailService.sendHtml(to, subject, emailTemplate, variables);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Weekly Digest
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendWeeklyDigest(Long userId) {
        try {
            WeeklyDigestData data = weeklyDigest.buildForUser(userId);
            sendWithOptionalPdf(
                    data.getUserEmail(),
                    "📽️ Seu resumo semanal no CineLog",
                    "weekly-digest", "weekly-digest",
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

    /**
     * Blocking variant for batch/task containers that shut down after job
     * completion.
     */
    public void sendWeeklyDigestToAllBlocking() {
        List<UserEntity> users = userRepository.findAll();
        log.info("[BATCH-SYNC] Sending weekly digest to {} users", users.size());
        for (UserEntity u : users) {
            try {
                WeeklyDigestData data = weeklyDigest.buildForUser(u.getId());
                emailService.sendHtml(
                        data.getUserEmail(),
                        "\uD83C\uDFAC Seu resumo semanal no CineLog",
                        "weekly-digest",
                        Map.of("data", data));
            } catch (Exception e) {
                log.error("[BATCH-SYNC] Failed weekly digest for userId={}: {}", u.getId(), e.getMessage(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Rated
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendTopRated(String toEmail, int limit) {
        TopRatedData data = topRated.build(limit);
        sendWithOptionalPdf(
                toEmail,
                "⭐ Top " + limit + " mídias mais bem avaliadas no CineLog",
                "top-rated", "top-rated",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Recommendations
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendRecommendations(Long userId) {
        try {
            RecommendationsData data = recommendations.buildForUser(userId);
            sendWithOptionalPdf(
                    data.getUserEmail(),
                    "🎬 Recomendações personalizadas para você no CineLog",
                    "recommendations", "recommendations",
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
        sendWithOptionalPdf(
                toEmail,
                "🔥 Em alta esta semana no CineLog",
                "trending", "trending",
                Map.of("data", data));
    }

    @Async("reportTaskExecutor")
    public void sendTrendingToAll() {
        List<UserEntity> users = userRepository.findAll();
        log.info("Sending trending report to {} users", users.size());
        users.forEach(u -> sendTrending(u.getEmail()));
    }

    /** Blocking variant for batch/task containers. */
    public void sendTrendingToAllBlocking() {
        List<UserEntity> users = userRepository.findAll();
        log.info("[BATCH-SYNC] Sending trending report to {} users", users.size());
        for (UserEntity u : users) {
            try {
                TrendingData data = trending.build();
                emailService.sendHtml(
                        u.getEmail(),
                        "\uD83D\uDD25 Em alta esta semana no CineLog",
                        "trending",
                        Map.of("data", data));
            } catch (Exception e) {
                log.error("[BATCH-SYNC] Failed trending for {}: {}", u.getEmail(), e.getMessage(), e);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Platform Report (admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendPlatformReport(String toEmail) {
        PlatformReportData data = platform.build();
        sendWithOptionalPdf(
                toEmail,
                "📊 Relatório da plataforma CineLog",
                "platform-report", "platform-report",
                Map.of("data", data));
    }

    /** Blocking variant for batch/task containers. */
    public void sendPlatformReportBlocking(String toEmail) {
        log.info("[BATCH-SYNC] Sending platform report to {}", toEmail);
        PlatformReportData data = platform.build();
        emailService.sendHtml(
                toEmail,
                "\uD83D\uDCCA Relat\u00F3rio da plataforma CineLog",
                "platform-report",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Actors (actors with highest-rated films)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendTopActors(String toEmail, int limit) {
        TopActorsData data = topActors.build(limit);
        sendWithOptionalPdf(
                toEmail,
                "🌟 Top " + limit + " atores com filmes mais bem avaliados no CineLog",
                "top-actors", "top-actors",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New Releases (recently added media)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendNewReleases(String toEmail, int days, int limit) {
        NewReleasesData data = newReleases.build(days, limit);
        sendWithOptionalPdf(
                toEmail,
                "🎬 " + data.getTotalNewMedia() + " novos títulos nos últimos " + days + " dias no CineLog",
                "new-releases", "new-releases",
                Map.of("data", data));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genre Spotlight (deep dive into a genre)
    // ─────────────────────────────────────────────────────────────────────────

    @Async("reportTaskExecutor")
    public void sendGenreSpotlight(String toEmail, String genreName) {
        GenreSpotlightData data = genreSpotlight.build(genreName);
        sendWithOptionalPdf(
                toEmail,
                "🎯 Gênero em destaque: " + data.getGenreName() + " — CineLog",
                "genre-spotlight", "genre-spotlight",
                Map.of("data", data));
    }
}
