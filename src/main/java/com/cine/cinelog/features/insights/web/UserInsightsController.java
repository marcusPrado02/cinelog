package com.cine.cinelog.features.insights.web;

import com.cine.cinelog.core.application.services.UserInsightsService;
import com.cine.cinelog.features.insights.web.dto.UserInsightsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Controller REST para endpoints de insights e estatísticas de usuários.
 *
 * <p>
 * <strong>Feature:</strong> User Insights (PR6 - CQRS Read Model)
 *
 * <p>
 * <strong>Endpoints:</strong>
 * <ul>
 * <li>GET /api/users/{userId}/insights - Estatísticas completas do usuário</li>
 * </ul>
 *
 * <p>
 * <strong>Eventual Consistency:</strong> Dados são atualizados de forma
 * assíncrona
 * por consumidores Kafka. Pode haver delay entre ação (criar WatchEntry) e
 * atualização.
 *
 * @since 1.0 (PR6)
 * @see UserInsightsService
 * @see com.cine.cinelog.infrastructure.messaging.kafka.consumer.UserStatsUpdater
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Insights", description = "Estatísticas e insights de usuários (CQRS Read Model)")
@PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
public class UserInsightsController {

    private static final Logger log = LoggerFactory.getLogger(UserInsightsController.class);

    private final UserInsightsService userInsightsService;

    public UserInsightsController(UserInsightsService userInsightsService) {
        this.userInsightsService = userInsightsService;
    }

    /**
     * Retorna estatísticas completas de um usuário.
     *
     * <p>
     * <strong>Eventual consistency:</strong> Dados podem estar desatualizados
     * se WatchEntry foi criado recentemente (delay de processamento Kafka).
     *
     * @param userId ID do usuário
     * @return estatísticas agregadas do usuário
     */
    @GetMapping("/{userId}/insights")
    @Operation(summary = "Obter insights do usuário", description = "Retorna estatísticas agregadas de visualização do usuário. "
            +
            "Dados atualizados de forma assíncrona via eventos Kafka (eventual consistency).", responses = {
                    @ApiResponse(responseCode = "200", description = "Insights retornados com sucesso", content = @Content(schema = @Schema(implementation = UserInsightsResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não possui estatísticas registradas")
            })
    public ResponseEntity<UserInsightsResponse> getUserInsights(
            @Parameter(description = "ID do usuário", example = "123") @PathVariable Long userId) {

        log.debug("GET /api/users/{}/insights", userId);

        Map<String, Object> insights = userInsightsService.getUserInsights(userId);

        UserInsightsResponse response = new UserInsightsResponse(
                toLong(insights.get("userId")),
                toLong(insights.get("totalWatched")),
                toLong(insights.get("totalMovies")),
                toLong(insights.get("totalSeries")),
                toBigDecimal(insights.get("avgRating")),
                toLocalDate(insights.get("lastWatchedAt")),
                toLocalDateTime(insights.get("updatedAt")));

        log.debug("Insights retornados: totalWatched={}, avgRating={}",
                response.totalWatched(), response.avgRating());

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica se usuário tem estatísticas registradas.
     *
     * @param userId ID do usuário
     * @return 200 se tem stats, 404 caso contrário
     */
    @GetMapping("/{userId}/insights/exists")
    @Operation(summary = "Verificar se usuário tem estatísticas", description = "Verifica se o usuário possui estatísticas registradas na base CQRS.", responses = {
            @ApiResponse(responseCode = "200", description = "Usuário tem estatísticas"),
            @ApiResponse(responseCode = "404", description = "Usuário não tem estatísticas")
    })
    public ResponseEntity<Void> checkUserStatsExist(
            @Parameter(description = "ID do usuário", example = "123") @PathVariable Long userId) {

        boolean hasStats = userInsightsService.hasStats(userId);

        return hasStats
                ? ResponseEntity.ok().build()
                : ResponseEntity.notFound().build();
    }

    // ── Safe type conversions (Redis deserialization may return unexpected types) ──

    private Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Long l) return l;
        if (val instanceof Number n) return n.longValue();
        return Long.valueOf(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        if (val instanceof BigDecimal bd) return bd;
        if (val instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(val.toString());
    }

    private LocalDate toLocalDate(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof String s) return LocalDate.parse(s);
        return null;
    }

    private LocalDateTime toLocalDateTime(Object val) {
        if (val == null) return null;
        if (val instanceof LocalDateTime ldt) return ldt;
        if (val instanceof String s) return LocalDateTime.parse(s);
        return null;
    }
}
