package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO de resposta para busca de mídias.
 *
 * <p>
 * Representa uma mídia retornada em endpoints de busca.
 * Contém informações essenciais para exibição em listas/grids.
 *
 * @param id            ID único da mídia
 * @param title         título original
 * @param overview      descrição/sinopse
 * @param type          tipo (MOVIE ou SERIES)
 * @param releaseYear   ano de lançamento
 * @param averageRating rating médio (0.0 - 10.0)
 * @param posterPath    caminho do poster (TMDB)
 * @param backdropPath  caminho do backdrop (TMDB)
 * @param genres        lista de gêneros
 *
 * @since 1.0 (PR6)
 */
@Schema(description = "Resposta de busca de mídia")
public record MediaSearchResponse(
        @Schema(description = "ID único da mídia", example = "123") Long id,

        @Schema(description = "Título original", example = "The Matrix") String title,

        @Schema(description = "Descrição/Sinopse", example = "A computer hacker learns...") String overview,

        @Schema(description = "Tipo de mídia", example = "MOVIE") MediaType type,

        @Schema(description = "Ano de lançamento", example = "1999") Integer releaseYear,

        @Schema(description = "Rating médio (0.0 - 10.0)", example = "8.7") BigDecimal averageRating,

        @Schema(description = "Caminho do poster (TMDB)", example = "/poster123.jpg") String posterPath,

        @Schema(description = "Caminho do backdrop (TMDB)", example = "/backdrop123.jpg") String backdropPath,

        @Schema(description = "Lista de gêneros") List<GenreResponse> genres) {
    /**
     * DTO de gênero simplificado.
     */
    @Schema(description = "Gênero da mídia")
    public record GenreResponse(
            @Schema(description = "ID do gênero", example = "28") Long id,

            @Schema(description = "Nome do gênero", example = "Action") String name) {
    }
}
