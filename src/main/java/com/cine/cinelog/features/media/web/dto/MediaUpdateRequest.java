package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;
import jakarta.validation.constraints.*;

/**
 * DTO de requisição para atualização de uma mídia existente.
 * 
 * <p>
 * Permite atualizar todos os dados da mídia:
 * <ul>
 * <li>title: título da mídia (obrigatório, máx. 300 caracteres)</li>
 * <li>type: tipo de mídia - MOVIE ou SERIES (obrigatório)</li>
 * <li>releaseYear: ano de lançamento (entre 1800 e 2100)</li>
 * <li>originalTitle: título original (máx. 300 caracteres)</li>
 * <li>originalLanguage: idioma original (máx. 10 caracteres)</li>
 * <li>posterUrl: URL da imagem do poster (máx. 300 caracteres)</li>
 * <li>backdropUrl: URL da imagem de fundo (máx. 300 caracteres)</li>
 * <li>overview: sinopse/descrição da mídia</li>
 * <li>tmdbId: ID da mídia no TMDB</li>
 * </ul>
 * 
 * <p>
 * Validações: título e tipo são obrigatórios em toda atualização.
 * 
 * @since 1.0
 */
public record MediaUpdateRequest(
                @NotBlank @Size(max = 300) String title,
                @NotNull MediaType type,
                @Min(1800) @Max(2100) Integer releaseYear,
                @Size(max = 300) String originalTitle,
                @Size(max = 10) String originalLanguage,
                @Size(max = 300) String posterUrl,
                @Size(max = 300) String backdropUrl,
                String overview,
                Integer tmdbId) {
}
