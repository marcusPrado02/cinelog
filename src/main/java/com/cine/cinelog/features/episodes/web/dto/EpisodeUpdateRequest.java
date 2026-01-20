package com.cine.cinelog.features.episodes.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização de episódio existente.
 * 
 * <p>
 * Permite atualizar dados do episódio (não permite alterar seasonId):
 * <ul>
 * <li>episodeNumber: número do episódio (obrigatório, mínimo 1)</li>
 * <li>name: nome/título do episódio (opcional, máx. 200 caracteres)</li>
 * <li>airDate: data de exibição do episódio (opcional)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record EpisodeUpdateRequest(
        @NotNull @Min(1) Integer episodeNumber,
        @Size(max = 200) String name,
        LocalDate airDate) {
}