package com.cine.cinelog.features.episodes.web.dto;

import java.time.LocalDate;

/**
 * DTO de resposta contendo informações completas de um episódio.
 * 
 * <p>
 * Retorna todos os dados do episódio de uma temporada:
 * <ul>
 * <li>id: identificador único do episódio</li>
 * <li>seasonId: ID da temporada associada</li>
 * <li>episodeNumber: número do episódio</li>
 * <li>name: nome/título do episódio</li>
 * <li>airDate: data de exibição do episódio</li>
 * </ul>
 * 
 * @since 1.0
 */
public record EpisodeResponse(Long id, Long seasonId, Integer episodeNumber, String name, LocalDate airDate) {
}