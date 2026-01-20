package com.cine.cinelog.features.recommendations.web;

import com.cine.cinelog.core.application.services.RecommendationService;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.recommendations.web.dto.RecommendationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller REST para recomendações de mídias.
 *
 * <p>
 * <strong>Feature:</strong> Recommendations (PR6 - Fase 6)
 *
 * <p>
 * Endpoints:
 * <ul>
 * <li>GET /api/users/{userId}/recommendations - Recomendações automáticas
 * (melhor estratégia)</li>
 * <li>GET /api/users/{userId}/recommendations/strategies - Lista estratégias
 * disponíveis</li>
 * <li>GET /api/users/{userId}/recommendations/{strategy} - Recomendações com
 * estratégia específica</li>
 * </ul>
 *
 * <p>
 * <strong>Estratégias disponíveis:</strong>
 * <ul>
 * <li><strong>content-based</strong>: Baseado em gêneros/atributos das mídias
 * assistidas</li>
 * <li><strong>collaborative</strong>: Baseado em usuários com gostos
 * similares</li>
 * <li><strong>hybrid</strong>: Combina múltiplas estratégias (recomendado)</li>
 * </ul>
 *
 * @since 1.0 (PR6 - Fase 6)
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Recommendations", description = "Recomendações personalizadas de mídias")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * Gera recomendações automáticas usando a melhor estratégia disponível.
     *
     * <p>
     * Ordem de prioridade:
     * 1. Hybrid (combina Content-Based + Collaborative)
     * 2. Content-Based (se houver histórico do usuário)
     * 3. Collaborative (se houver usuários similares)
     *
     * @param userId ID do usuário
     * @param limit  máximo de recomendações (1-100, padrão: 20)
     * @return lista de recomendações
     */
    @GetMapping("/{userId}/recommendations")
    @Operation(summary = "Recomendações automáticas", description = "Gera recomendações usando a melhor estratégia disponível. "
            +
            "Prioriza Hybrid (melhor qualidade), depois Content-Based e Collaborative.", responses = {
                    @ApiResponse(responseCode = "200", description = "Recomendações geradas com sucesso", content = @Content(schema = @Schema(implementation = RecommendationResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            })
    public ResponseEntity<List<RecommendationResponse>> getRecommendations(
            @Parameter(description = "ID do usuário", example = "123", required = true) @PathVariable Long userId,

            @Parameter(description = "Máximo de recomendações (1-100)", example = "20") @RequestParam(required = false, defaultValue = "20") Integer limit) {
        List<Media> recommendations = recommendationService.getRecommendations(userId, limit);

        if (recommendations.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        List<RecommendationResponse> response = recommendations.stream()
                .map(media -> toResponse(media, "Recomendação automática baseada no seu histórico"))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * Gera recomendações usando estratégia específica.
     *
     * @param userId   ID do usuário
     * @param strategy nome da estratégia (content-based, collaborative, hybrid)
     * @param limit    máximo de recomendações
     * @return lista de recomendações
     */
    @GetMapping("/{userId}/recommendations/{strategy}")
    @Operation(summary = "Recomendações com estratégia específica", description = "Gera recomendações usando o algoritmo especificado. "
            +
            "Estratégias disponíveis: content-based, collaborative, hybrid.", responses = {
                    @ApiResponse(responseCode = "200", description = "Recomendações geradas com sucesso", content = @Content(schema = @Schema(implementation = RecommendationResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Estratégia inválida"),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            })
    public ResponseEntity<List<RecommendationResponse>> getRecommendationsByStrategy(
            @Parameter(description = "ID do usuário", example = "123", required = true) @PathVariable Long userId,

            @Parameter(description = "Nome da estratégia", example = "hybrid", schema = @Schema(allowableValues = {
                    "content-based", "collaborative", "hybrid" }), required = true) @PathVariable String strategy,

            @Parameter(description = "Máximo de recomendações (1-100)", example = "20") @RequestParam(required = false, defaultValue = "20") Integer limit) {
        try {
            List<Media> recommendations = recommendationService.getRecommendationsByStrategy(userId, strategy, limit);

            if (recommendations.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            String reason = getReasonByStrategy(strategy);
            List<RecommendationResponse> response = recommendations.stream()
                    .map(media -> toResponse(media, reason))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Lista estratégias disponíveis para um usuário.
     *
     * @param userId ID do usuário
     * @return lista de nomes de estratégias
     */
    @GetMapping("/{userId}/recommendations/strategies")
    @Operation(summary = "Lista estratégias disponíveis", description = "Retorna quais algoritmos de recomendação estão disponíveis "
            +
            "para o usuário com base no histórico e dados disponíveis.", responses = {
                    @ApiResponse(responseCode = "200", description = "Estratégias listadas com sucesso", content = @Content(schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário não encontrado")
            })
    public ResponseEntity<List<String>> getAvailableStrategies(
            @Parameter(description = "ID do usuário", example = "123", required = true) @PathVariable Long userId) {
        List<String> strategies = recommendationService.getAvailableStrategies(userId);
        return ResponseEntity.ok(strategies);
    }

    /**
     * Converte Media para DTO de resposta.
     */
    private RecommendationResponse toResponse(Media media, String reason) {
        // Score simulado (em produção viria do algoritmo)
        double score = 0.75 + (Math.random() * 0.25); // 0.75 - 1.0

        return new RecommendationResponse(
                media.getId(),
                media.getTitle(),
                media.getType(),
                media.getReleaseYear(),
                null, // averageRating - Media não tem esse campo exposto
                media.getPosterUrl(),
                List.of(), // genres - Media não tem getGenres()
                score,
                reason);
    }

    /**
     * Retorna razão da recomendação por estratégia.
     */
    private String getReasonByStrategy(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "content-based", "content_based" ->
                "Baseado em gêneros e características das mídias que você assistiu";
            case "collaborative" ->
                "Baseado em usuários com gostos similares aos seus";
            case "hybrid" ->
                "Recomendação híbrida combinando múltiplos algoritmos";
            default ->
                "Recomendado para você";
        };
    }
}
