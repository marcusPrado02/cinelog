package com.cine.cinelog.features.seasons.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO de requisição para criação de temporada de série.
 * 
 * <p>
 * Temporadas organizam episódios de uma série:
 * <ul>
 * <li>mediaId: ID da série/mídia (obrigatório)</li>
 * <li>seasonNumber: número da temporada, começando em 0 (obrigatório, mínimo
 * 0)</li>
 * <li>name: nome da temporada (opcional, máx. 200 caracteres)</li>
 * <li>airDate: data de estreia da temporada (opcional)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record SeasonCreateRequest(
                @NotNull Long mediaId,
                @NotNull @Min(0) Integer seasonNumber,
                @Size(max = 200) String name,
                LocalDate airDate) {
}
