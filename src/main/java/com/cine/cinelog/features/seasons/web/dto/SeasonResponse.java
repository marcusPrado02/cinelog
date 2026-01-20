package com.cine.cinelog.features.seasons.web.dto;

import java.time.LocalDate;

/**
 * DTO de resposta contendo informações completas de uma temporada.
 * 
 * <p>
 * Retorna todos os dados da temporada de uma série:
 * <ul>
 * <li>id: identificador único da temporada</li>
 * <li>mediaId: ID da série/mídia associada</li>
 * <li>seasonNumber: número da temporada</li>
 * <li>name: nome da temporada</li>
 * <li>airDate: data de estreia da temporada</li>
 * </ul>
 * 
 * @since 1.0
 */
public record SeasonResponse(Long id, Long mediaId, Integer seasonNumber, String name, LocalDate airDate) {
}