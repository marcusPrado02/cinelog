package com.cine.cinelog.features.popularity.web;

import com.cine.cinelog.core.application.services.MediaPopularityService;
import com.cine.cinelog.core.application.services.MediaPopularityService.TrendingPeriod;
import com.cine.cinelog.features.popularity.web.dto.MediaPopularityResponse;
import com.cine.cinelog.features.readmodels.persistence.entity.MediaPopularityEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para endpoints de popularidade e trending de mídias.
 *
 * <p>
 * <strong>Feature:</strong> MediaPopularity (PR6 - CQRS Read Model)
 *
 * <p>
 * <strong>Endpoints:</strong>
 * <ul>
 * <li>GET /api/media/top-rated - Top mídias com maior rating</li>
 * <li>GET /api/media/trending - Mídias trending (mais assistidas
 * recentemente)</li>
 * <li>GET /api/media/most-watched - Mídias mais assistidas de todos os
 * tempos</li>
 * </ul>
 *
 * <p>
 * <strong>Eventual Consistency:</strong> Dados são atualizados de forma
 * assíncrona
 * por consumidores Kafka. Pode haver delay entre visualização e atualização das
 * métricas.
 *
 * @since 1.0 (PR6)
 * @see MediaPopularityService
 * @see com.cine.cinelog.infrastructure.messaging.kafka.consumer.MediaPopularityUpdater
 */
@RestController
@RequestMapping("/api/media")
@Tag(name = "Media Popularity", description = "Popularidade e tendências de mídias (CQRS Read Model)")
public class MediaPopularityController {

    private static final Logger log = LoggerFactory.getLogger(MediaPopularityController.class);

    private final MediaPopularityService mediaPopularityService;

    public MediaPopularityController(MediaPopularityService mediaPopularityService) {
        this.mediaPopularityService = mediaPopularityService;
    }

    /**
     * Retorna top N mídias com maior média de rating.
     *
     * <p>
     * Critérios:
     * <ul>
     * <li>Mínimo de 3 avaliações (evitar ratings inflados)</li>
     * <li>Ordenado por avg_rating DESC</li>
     * </ul>
     *
     * @param limit número máximo de resultados (default: 20, max: 100)
     * @return lista de mídias top rated
     */
    @GetMapping("/top-rated")
    @Operation(summary = "Obter mídias top rated", description = "Retorna top N mídias com maior média de rating (mínimo 3 avaliações). "
            +
            "Dados atualizados de forma assíncrona via eventos Kafka.", responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de mídias retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MediaPopularityResponse.class))))
            })
    public ResponseEntity<List<MediaPopularityResponse>> getTopRated(
            @Parameter(description = "Número máximo de resultados (default: 20, max: 100)", example = "20") @RequestParam(defaultValue = "20") int limit) {

        log.debug("GET /api/media/top-rated?limit={}", limit);

        List<MediaPopularityEntity> topRated = mediaPopularityService.getTopRated(limit);

        List<MediaPopularityResponse> response = topRated.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Top rated retornou {} mídias", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Retorna mídias trending (mais assistidas recentemente).
     *
     * <p>
     * Períodos suportados:
     * <ul>
     * <li>7d (WEEK): últimos 7 dias</li>
     * <li>30d (MONTH): últimos 30 dias</li>
     * <li>all (ALL_TIME): desde sempre</li>
     * </ul>
     *
     * @param period período para trending (default: 7d)
     * @param limit  número máximo de resultados (default: 20)
     * @return lista de mídias trending
     */
    @GetMapping("/trending")
    @Operation(summary = "Obter mídias trending", description = "Retorna mídias mais assistidas recentemente no período especificado. "
            +
            "Ordenado por watch_count DESC, last_watched_at DESC.", responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de mídias retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MediaPopularityResponse.class)))),
                    @ApiResponse(responseCode = "400", description = "Período inválido (valores permitidos: 7d, 30d, all)")
            })
    public ResponseEntity<List<MediaPopularityResponse>> getTrending(
            @Parameter(description = "Período (7d, 30d, all)", example = "7d") @RequestParam(defaultValue = "7d") String period,
            @Parameter(description = "Número máximo de resultados", example = "20") @RequestParam(defaultValue = "20") int limit) {

        String safePeriod = period == null ? "null" : period.replace('\n', '_').replace('\r', '_');
        log.debug("GET /api/media/trending?period={}&limit={}", safePeriod, limit);

        TrendingPeriod trendingPeriod = parsePeriod(period);

        List<MediaPopularityEntity> trending = mediaPopularityService.getTrending(trendingPeriod, limit);

        List<MediaPopularityResponse> response = trending.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Trending retornou {} mídias", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Retorna mídias mais assistidas de todos os tempos.
     *
     * @param limit número máximo de resultados (default: 20)
     * @return lista de mídias ordenada por watch_count DESC
     */
    @GetMapping("/most-watched")
    @Operation(summary = "Obter mídias mais assistidas", description = "Retorna mídias com maior número de visualizações de todos os tempos. "
            +
            "Ordenado por watch_count DESC.", responses = {
                    @ApiResponse(responseCode = "200", description = "Lista de mídias retornada com sucesso", content = @Content(array = @ArraySchema(schema = @Schema(implementation = MediaPopularityResponse.class))))
            })
    public ResponseEntity<List<MediaPopularityResponse>> getMostWatched(
            @Parameter(description = "Número máximo de resultados", example = "20") @RequestParam(defaultValue = "20") int limit) {

        log.debug("GET /api/media/most-watched?limit={}", limit);

        List<MediaPopularityEntity> mostWatched = mediaPopularityService.getMostWatched(limit);

        List<MediaPopularityResponse> response = mostWatched.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        log.debug("Most watched retornou {} mídias", response.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Converte entity para DTO response.
     */
    private MediaPopularityResponse toResponse(MediaPopularityEntity entity) {
        return new MediaPopularityResponse(
                entity.getMediaId(),
                entity.getWatchCount(),
                entity.getRatingsCount(),
                entity.getAvgRating(),
                entity.getLastWatchedAt(),
                entity.getUpdatedAt());
    }

    /**
     * Converte string de período para enum.
     */
    private TrendingPeriod parsePeriod(String period) {
        return switch (period.toLowerCase()) {
            case "7d", "week" -> TrendingPeriod.WEEK;
            case "30d", "month" -> TrendingPeriod.MONTH;
            case "all", "all_time" -> TrendingPeriod.ALL_TIME;
            default -> {
                String rawPeriod = period == null ? "null" : period;
                StringBuilder sanitized = new StringBuilder(rawPeriod.length());
                for (int i = 0; i < rawPeriod.length(); i++) {
                    char ch = rawPeriod.charAt(i);
                    if (ch == '\n' || ch == '\r' || Character.isISOControl(ch)) {
                        sanitized.append('_');
                    } else {
                        sanitized.append(ch);
                    }
                }
                String safePeriod = sanitized.toString();
                log.warn("Período inválido: {}. Usando default: WEEK", safePeriod);
                yield TrendingPeriod.WEEK;
            }
        };
    }
}
