package com.cine.cinelog.features.credits.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO de requisição para criação de crédito (associação de pessoa a mídia).
 * 
 * <p>
 * Créditos representam a participação de pessoas em mídias (atores, diretores,
 * etc.):
 * <ul>
 * <li>mediaId: ID da mídia (obrigatório)</li>
 * <li>personId: ID da pessoa (obrigatório)</li>
 * <li>role: papel/função (obrigatório - ex: "Actor", "Director", "Writer")</li>
 * <li>characterName: nome do personagem interpretado (opcional, máx. 200
 * caracteres)</li>
 * <li>orderIndex: ordem de aparição nos créditos (0 a 32767)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record CreditCreateRequest(
        @NotNull Long mediaId,
        @NotNull Long personId,
        @NotBlank String role,
        @Size(max = 200) String characterName,
        @Min(0) @Max(32767) Short orderIndex) {
}
