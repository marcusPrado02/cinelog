package com.cine.cinelog.features.credits.web.dto;

import jakarta.validation.constraints.*;

/**
 * DTO de requisição para atualização de crédito existente.
 * 
 * <p>
 * Permite atualizar informações do crédito (não permite alterar mediaId e
 * personId):
 * <ul>
 * <li>role: papel/função (obrigatório - ex: "Actor", "Director", "Writer")</li>
 * <li>characterName: nome do personagem interpretado (opcional, máx. 200
 * caracteres)</li>
 * <li>orderIndex: ordem de aparição nos créditos (0 a 32767)</li>
 * </ul>
 * 
 * @since 1.0
 */
public record CreditUpdateRequest(
        @NotBlank String role,
        @Size(max = 200) String characterName,
        @Min(0) @Max(32767) Short orderIndex) {
}