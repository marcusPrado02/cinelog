package com.cine.cinelog.features.seasons.web.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * DTO de requisição para atualização de temporada existente.
 * 
 * <p>
 * Permite atualizar dados da temporada (não permite alterar mediaId):
 * <ul>
 * <li>seasonNumber: número da temporada (obrigatório, mínimo 0)</li>
 * <li>name: nome da temporada (opcional, máx. 200 caracteres)</li>
 * <li>airDate: data de estreia da temporada (opcional)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record SeasonUpdateRequest(
                @NotNull @Min(0) Integer seasonNumber,
                @Size(max = 200) String name,
                LocalDate airDate) {
}