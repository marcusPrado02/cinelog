package com.cine.cinelog.features.recommendations.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resposta para recomendações de mídias.
 *
 * <p>
 * <strong>Feature:</strong> Recommendations (PR6 - Fase 6)
 *
 * @param id                  ID da mídia
 * @param title               título
 * @param type                tipo (MOVIE/SERIES)
 * @param releaseYear         ano de lançamento
 * @param averageRating       rating médio
 * @param posterUrl           URL do poster
 * @param genres              lista de gêneros
 * @param recommendationScore score de recomendação (0.0 - 1.0)
 * @param reason              motivo da recomendação
 *
 * @since 1.0 (PR6 - Fase 6)
 */
@Schema(description = "Resposta de recomendação de mídia")
public record RecommendationResponse(
        @Schema(description = "ID da mídia", example = "123") Long id,

        @Schema(description = "Título", example = "Inception") String title,

        @Schema(description = "Tipo de mídia", example = "MOVIE") MediaType type,

        @Schema(description = "Ano de lançamento", example = "2010") Integer releaseYear,

        @Schema(description = "Rating médio", example = "8.8") BigDecimal averageRating,

        @Schema(description = "URL do poster", example = "/poster123.jpg") String posterUrl,

        @Schema(description = "Lista de gêneros") List<String> genres,

        @Schema(description = "Score de recomendação (0.0 - 1.0)", example = "0.85") Double recommendationScore,

        @Schema(description = "Motivo da recomendação", example = "Baseado em gêneros que você gosta: Action, Sci-Fi") String reason) {
}
