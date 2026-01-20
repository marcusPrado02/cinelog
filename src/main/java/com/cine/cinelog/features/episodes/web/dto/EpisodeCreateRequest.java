package com.cine.cinelog.features.episodes.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO de requisição para criação de episódio de uma temporada.
 * 
 * <p>
 * Episódios pertencem a uma temporada de série:
 * <ul>
 * <li>seasonId: ID da temporada (obrigatório)</li>
 * <li>episodeNumber: número do episódio, começando em 1 (obrigatório, mínimo
 * 1)</li>
 * <li>name: nome/título do episódio (opcional, máx. 200 caracteres)</li>
 * <li>airDate: data de exibição do episódio (opcional)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record EpisodeCreateRequest(
        @NotNull Long seasonId,
        @NotNull @Min(1) Integer episodeNumber,
        @Size(max = 200) String name,
        LocalDate airDate) {
}