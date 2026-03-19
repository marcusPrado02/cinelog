package com.cine.cinelog.features.reports.web;

import com.cine.cinelog.features.reports.data.*;
import com.cine.cinelog.features.reports.email.ReportEmailService;
import com.cine.cinelog.features.reports.query.*;
import com.cine.cinelog.features.reports.web.dto.SendReportRequest;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;
import com.cine.cinelog.shared.security.CinelogUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST endpoints para geração e envio de relatórios por e-mail.
 *
 * <p><strong>Endpoints de usuário</strong> (qualquer usuário autenticado):
 * <ul>
 *   <li>GET  /api/v1/reports/weekly-digest   — preview do digest semanal</li>
 *   <li>POST /api/v1/reports/weekly-digest   — envia digest por e-mail</li>
 *   <li>GET  /api/v1/reports/top-rated       — preview dos mais bem avaliados</li>
 *   <li>POST /api/v1/reports/top-rated       — envia top-rated por e-mail</li>
 *   <li>GET  /api/v1/reports/recommendations — preview de recomendações</li>
 *   <li>POST /api/v1/reports/recommendations — envia recomendações por e-mail</li>
 *   <li>GET  /api/v1/reports/trending        — preview dos em alta</li>
 *   <li>POST /api/v1/reports/trending        — envia trending por e-mail</li>
 * </ul>
 *
 * <p><strong>Endpoints de admin</strong>:
 * <ul>
 *   <li>GET  /api/v1/admin/reports/platform    — preview métricas da plataforma</li>
 *   <li>POST /api/v1/admin/reports/platform    — envia relatório ao admin</li>
 *   <li>POST /api/v1/admin/reports/send-to-all — envia trending para todos os usuários</li>
 * </ul>
 *
 * <p>Todos os envios de e-mail são assíncronos; os endpoints retornam
 * {@code 202 Accepted} imediatamente.</p>
 *
 * @since 1.0
 */
@RestController
@Validated
@Tag(name = "Reports", description = "Geração e envio de relatórios por e-mail")
@SecurityRequirement(name = "BearerAuth")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportEmailService reportEmailService;
    private final WeeklyDigestQueryService weeklyDigestQuery;
    private final TopRatedQueryService topRatedQuery;
    private final RecommendationsQueryService recommendationsQuery;
    private final TrendingQueryService trendingQuery;
    private final PlatformReportQueryService platformQuery;
    private final TopActorsQueryService topActorsQuery;
    private final NewReleasesQueryService newReleasesQuery;
    private final GenreSpotlightQueryService genreSpotlightQuery;

    public ReportController(ReportEmailService reportEmailService,
                            WeeklyDigestQueryService weeklyDigestQuery,
                            TopRatedQueryService topRatedQuery,
                            RecommendationsQueryService recommendationsQuery,
                            TrendingQueryService trendingQuery,
                            PlatformReportQueryService platformQuery,
                            TopActorsQueryService topActorsQuery,
                            NewReleasesQueryService newReleasesQuery,
                            GenreSpotlightQueryService genreSpotlightQuery) {
        this.reportEmailService = reportEmailService;
        this.weeklyDigestQuery = weeklyDigestQuery;
        this.topRatedQuery = topRatedQuery;
        this.recommendationsQuery = recommendationsQuery;
        this.trendingQuery = trendingQuery;
        this.platformQuery = platformQuery;
        this.topActorsQuery = topActorsQuery;
        this.newReleasesQuery = newReleasesQuery;
        this.genreSpotlightQuery = genreSpotlightQuery;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Relatórios de usuário — preview (GET) + envio por e-mail (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/reports/weekly-digest")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.weekly_digest.preview")
    @Operation(summary = "Preview do digest semanal do usuário autenticado", responses = {
            @ApiResponse(responseCode = "200", description = "Dados do digest"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<WeeklyDigestData> previewWeeklyDigest(
            @AuthenticationPrincipal CinelogUserDetails user) {
        log.debug("Iniciando previewWeeklyDigest. Parâmetros: {}", Map.of("userId", user.getUserId()));
        try {
            WeeklyDigestData data = weeklyDigestQuery.buildForUser(user.getUserId());
            log.info("Preview do digest semanal gerado com sucesso. UserId: {}", user.getUserId());
            log.debug("Finalizando previewWeeklyDigest. Resultado: {} itens", data.getRecentlyWatched() != null ? data.getRecentlyWatched().size() : 0);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Erro ao gerar preview do digest semanal. Parâmetros: {}. Erro: {}",
                    Map.of("userId", user.getUserId()), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/reports/weekly-digest")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.weekly_digest.send")
    @Operation(summary = "Envia digest semanal por e-mail ao usuário autenticado", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendWeeklyDigest(
            @AuthenticationPrincipal CinelogUserDetails user) {
        log.debug("Iniciando sendWeeklyDigest. Parâmetros: {}", Map.of("userId", user.getUserId()));
        try {
            reportEmailService.sendWeeklyDigest(user.getUserId());
            log.info("Digest semanal enfileirado com sucesso. UserId: {}", user.getUserId());
            return ResponseEntity.accepted().body(Map.of("message", "Digest semanal enfileirado para envio."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar digest semanal. Parâmetros: {}. Erro: {}",
                    Map.of("userId", user.getUserId()), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/api/v1/reports/top-rated")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.top_rated.preview")
    @Operation(summary = "Preview dos títulos mais bem avaliados", responses = {
            @ApiResponse(responseCode = "200", description = "Dados dos top-rated"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<TopRatedData> previewTopRated(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Iniciando previewTopRated. Parâmetros: {}", Map.of("limit", limit));
        try {
            TopRatedData data = topRatedQuery.build(limit);
            log.info("Preview top-rated gerado com sucesso. Quantidade: {}", limit);
            log.debug("Finalizando previewTopRated. Resultado: {} itens", data.getItems() != null ? data.getItems().size() : 0);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Erro ao gerar preview top-rated. Parâmetros: {}. Erro: {}",
                    Map.of("limit", limit), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/reports/top-rated")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.top_rated.send")
    @Operation(summary = "Envia relatório de top-rated por e-mail ao usuário autenticado", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendTopRated(
            @AuthenticationPrincipal CinelogUserDetails user,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        int limit = body != null ? body.effectiveLimit() : 10;
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendTopRated. Parâmetros: {}", Map.of("toEmail", toEmail, "limit", limit));
        try {
            reportEmailService.sendTopRated(toEmail, limit);
            log.info("Relatório top-rated enfileirado com sucesso. Para: {}", toEmail);
            return ResponseEntity.accepted().body(Map.of("message", "Relatório top-rated enfileirado para envio."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar relatório top-rated. Parâmetros: {}. Erro: {}",
                    Map.of("toEmail", toEmail, "limit", limit), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/api/v1/reports/recommendations")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.recommendations.preview")
    @Operation(summary = "Preview de recomendações personalizadas do usuário autenticado", responses = {
            @ApiResponse(responseCode = "200", description = "Dados de recomendações"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<RecommendationsData> previewRecommendations(
            @AuthenticationPrincipal CinelogUserDetails user) {
        log.debug("Iniciando previewRecommendations. Parâmetros: {}", Map.of("userId", user.getUserId()));
        try {
            RecommendationsData data = recommendationsQuery.buildForUser(user.getUserId());
            log.info("Preview de recomendações gerado com sucesso. UserId: {}", user.getUserId());
            log.debug("Finalizando previewRecommendations. Resultado: {} itens", data.getRecommendations() != null ? data.getRecommendations().size() : 0);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Erro ao gerar preview de recomendações. Parâmetros: {}. Erro: {}",
                    Map.of("userId", user.getUserId()), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/reports/recommendations")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.recommendations.send")
    @Operation(summary = "Envia recomendações personalizadas por e-mail ao usuário autenticado", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendRecommendations(
            @AuthenticationPrincipal CinelogUserDetails user) {
        log.debug("Iniciando sendRecommendations. Parâmetros: {}", Map.of("userId", user.getUserId()));
        try {
            reportEmailService.sendRecommendations(user.getUserId());
            log.info("Recomendações enfileiradas com sucesso. UserId: {}", user.getUserId());
            return ResponseEntity.accepted().body(Map.of("message", "Recomendações enfileiradas para envio."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar recomendações. Parâmetros: {}. Erro: {}",
                    Map.of("userId", user.getUserId()), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/api/v1/reports/trending")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.trending.preview")
    @Operation(summary = "Preview dos títulos em alta (últimos N dias)", responses = {
            @ApiResponse(responseCode = "200", description = "Dados de trending"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<TrendingData> previewTrending(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Iniciando previewTrending. Parâmetros: {}", Map.of("days", days, "limit", limit));
        try {
            TrendingData data = trendingQuery.build(days, limit);
            log.info("Preview de trending gerado com sucesso. Dias: {}, Limite: {}", days, limit);
            log.debug("Finalizando previewTrending. Resultado: {} itens", data.getItems() != null ? data.getItems().size() : 0);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Erro ao gerar preview de trending. Parâmetros: {}. Erro: {}",
                    Map.of("days", days, "limit", limit), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/reports/trending")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.trending.send")
    @Operation(summary = "Envia relatório de trending por e-mail ao usuário autenticado", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendTrending(
            @AuthenticationPrincipal CinelogUserDetails user,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendTrending. Parâmetros: {}", Map.of("toEmail", toEmail));
        try {
            reportEmailService.sendTrending(toEmail);
            log.info("Relatório de trending enfileirado com sucesso. Para: {}", toEmail);
            return ResponseEntity.accepted().body(Map.of("message", "Relatório de trending enfileirado para envio."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar relatório de trending. Parâmetros: {}. Erro: {}",
                    Map.of("toEmail", toEmail), e.getMessage(), e);
            throw e;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top Actors — preview (GET) + envio por e-mail (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/reports/top-actors")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.top_actors.preview")
    @Operation(summary = "Preview dos atores com filmes mais bem avaliados", responses = {
            @ApiResponse(responseCode = "200", description = "Dados dos top actors"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<TopActorsData> previewTopActors(
            @RequestParam(defaultValue = "10") int limit) {
        log.debug("Iniciando previewTopActors. Parâmetros: {}", Map.of("limit", limit));
        TopActorsData data = topActorsQuery.build(limit);
        log.info("Preview top-actors gerado com sucesso. Limite: {}", limit);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/api/v1/reports/top-actors")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.top_actors.send")
    @Operation(summary = "Envia relatório de top actors por e-mail", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendTopActors(
            @AuthenticationPrincipal CinelogUserDetails user,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        int limit = body != null ? body.effectiveLimit() : 10;
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendTopActors. Parâmetros: {}", Map.of("toEmail", toEmail, "limit", limit));
        reportEmailService.sendTopActors(toEmail, limit);
        log.info("Relatório top-actors enfileirado com sucesso. Para: {}", toEmail);
        return ResponseEntity.accepted().body(Map.of("message", "Relatório top-actors enfileirado para envio."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // New Releases — preview (GET) + envio por e-mail (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/reports/new-releases")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.new_releases.preview")
    @Operation(summary = "Preview dos novos lançamentos adicionados ao catálogo", responses = {
            @ApiResponse(responseCode = "200", description = "Dados dos novos lançamentos"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<NewReleasesData> previewNewReleases(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "20") int limit) {
        log.debug("Iniciando previewNewReleases. Parâmetros: {}", Map.of("days", days, "limit", limit));
        NewReleasesData data = newReleasesQuery.build(days, limit);
        log.info("Preview new-releases gerado com sucesso. Dias: {}, Limite: {}", days, limit);
        return ResponseEntity.ok(data);
    }

    @PostMapping("/api/v1/reports/new-releases")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.new_releases.send")
    @Operation(summary = "Envia relatório de novos lançamentos por e-mail", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendNewReleases(
            @AuthenticationPrincipal CinelogUserDetails user,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendNewReleases. Parâmetros: {}", Map.of("toEmail", toEmail));
        reportEmailService.sendNewReleases(toEmail, 30, 20);
        log.info("Relatório new-releases enfileirado com sucesso. Para: {}", toEmail);
        return ResponseEntity.accepted().body(Map.of("message", "Relatório new-releases enfileirado para envio."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Genre Spotlight — preview (GET) + envio por e-mail (POST)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/reports/genre-spotlight")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.genre_spotlight.preview")
    @Operation(summary = "Preview do gênero em destaque (auto-seleciona ou especifica)", responses = {
            @ApiResponse(responseCode = "200", description = "Dados do gênero em destaque"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<GenreSpotlightData> previewGenreSpotlight(
            @RequestParam(required = false) String genre) {
        log.debug("Iniciando previewGenreSpotlight. Parâmetros: {}", Map.of("genre", genre != null ? genre : "auto"));
        GenreSpotlightData data = genreSpotlightQuery.build(genre);
        log.info("Preview genre-spotlight gerado com sucesso. Gênero: {}", data.getGenreName());
        return ResponseEntity.ok(data);
    }

    @PostMapping("/api/v1/reports/genre-spotlight")
    @PreAuthorize("isAuthenticated()")
    @Measured("cinelog.controller.reports.genre_spotlight.send")
    @Operation(summary = "Envia relatório de gênero em destaque por e-mail", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "401", description = "Não autenticado")
    })
    public ResponseEntity<Map<String, String>> sendGenreSpotlight(
            @AuthenticationPrincipal CinelogUserDetails user,
            @RequestParam(required = false) String genre,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendGenreSpotlight. Parâmetros: {}", Map.of("toEmail", toEmail, "genre", genre != null ? genre : "auto"));
        reportEmailService.sendGenreSpotlight(toEmail, genre);
        log.info("Relatório genre-spotlight enfileirado com sucesso. Para: {}", toEmail);
        return ResponseEntity.accepted().body(Map.of("message", "Relatório genre-spotlight enfileirado para envio."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Relatórios de admin
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/api/v1/admin/reports/platform")
    @PreAuthorize("hasRole('ADMIN')")
    @Measured("cinelog.controller.reports.platform.preview")
    @SecureOperation(module = "REPORTS", value = "ADMIN")
    @Operation(summary = "Preview das métricas da plataforma (somente admin)", responses = {
            @ApiResponse(responseCode = "200", description = "Dados da plataforma"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<PlatformReportData> previewPlatformReport() {
        log.debug("Iniciando previewPlatformReport.");
        try {
            PlatformReportData data = platformQuery.build();
            log.info("Preview do relatório de plataforma gerado com sucesso.");
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Erro ao gerar preview do relatório de plataforma. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/admin/reports/platform")
    @PreAuthorize("hasRole('ADMIN')")
    @Measured("cinelog.controller.reports.platform.send")
    @SecureOperation(module = "REPORTS", value = "ADMIN")
    @Operation(summary = "Envia relatório de métricas da plataforma por e-mail (somente admin)", responses = {
            @ApiResponse(responseCode = "202", description = "E-mail enfileirado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Map<String, String>> sendPlatformReport(
            @AuthenticationPrincipal CinelogUserDetails user,
            @Valid @RequestBody(required = false) SendReportRequest body) {
        String toEmail = (body != null && body.email() != null) ? body.email() : user.getUsername();
        log.debug("Iniciando sendPlatformReport. Parâmetros: {}", Map.of("toEmail", toEmail));
        try {
            reportEmailService.sendPlatformReport(toEmail);
            log.info("Relatório de plataforma enfileirado com sucesso. Para: {}", toEmail);
            return ResponseEntity.accepted().body(Map.of("message", "Relatório de plataforma enfileirado para envio."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar relatório de plataforma. Parâmetros: {}. Erro: {}",
                    Map.of("toEmail", toEmail), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/api/v1/admin/reports/send-to-all")
    @PreAuthorize("hasRole('ADMIN')")
    @Measured("cinelog.controller.reports.trending.send_to_all")
    @SecureOperation(module = "REPORTS", value = "ADMIN")
    @Operation(summary = "Envia relatório de trending a TODOS os usuários ativos (somente admin)", responses = {
            @ApiResponse(responseCode = "202", description = "E-mails enfileirados"),
            @ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<Map<String, String>> sendTrendingToAll() {
        log.debug("Iniciando sendTrendingToAll.");
        try {
            reportEmailService.sendTrendingToAll();
            log.info("Envio em massa de trending enfileirado com sucesso.");
            return ResponseEntity.accepted().body(Map.of("message", "Trending enfileirado para todos os usuários."));
        } catch (Exception e) {
            log.error("Erro ao enfileirar envio em massa de trending. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
