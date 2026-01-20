package com.cine.cinelog.features.media.web.dto;

import com.cine.cinelog.core.domain.enums.MediaType;

/**
 * DTO de resposta contendo informações completas de uma mídia.
 *
 * <p>
 * Retorna todos os dados da mídia cadastrada no sistema:
 * <ul>
 * <li>id: identificador único da mídia</li>
 * <li>title: título da mídia</li>
 * <li>type: tipo de mídia (MOVIE ou SERIES)</li>
 * <li>releaseYear: ano de lançamento</li>
 * <li>originalTitle: título original</li>
 * <li>originalLanguage: idioma original</li>
 * <li>posterUrl: URL da imagem do poster</li>
 * <li>backdropUrl: URL da imagem de fundo</li>
 * <li>overview: sinopse/descrição da mídia</li>
 * <li>tmdbId: ID da mídia no TMDB (The Movie Database)</li>
 * </ul>
 *
 * @since 1.0
 */
public record MediaResponse(
        Long id,
        String title,
        MediaType type,
        Integer releaseYear,
        String originalTitle,
        String originalLanguage,
        String posterUrl,
        String backdropUrl,
        String overview,
        Long tmdbId) {
}
