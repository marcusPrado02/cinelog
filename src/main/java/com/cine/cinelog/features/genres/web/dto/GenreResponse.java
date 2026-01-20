package com.cine.cinelog.features.genres.web.dto;

/**
 * DTO de resposta contendo informações de um gênero.
 * 
 * <p>
 * Retorna o identificador e nome do gênero:
 * <ul>
 * <li>id: identificador único do gênero (Short)</li>
 * <li>name: nome do gênero (ex: "Ação", "Drama", "Comédia")</li>
 * </ul>
 * 
 * @since 1.0
 */
public record GenreResponse(Short id, String name) {
}
