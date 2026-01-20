package com.cine.cinelog.features.credits.web.dto;

/**
 * DTO de resposta contendo informações completas de um crédito.
 * 
 * <p>
 * Retorna todos os dados do crédito (associação pessoa-mídia):
 * <ul>
 * <li>id: identificador único do crédito</li>
 * <li>mediaId: ID da mídia associada</li>
 * <li>personId: ID da pessoa associada</li>
 * <li>role: papel/função (ex: "Actor", "Director", "Writer")</li>
 * <li>characterName: nome do personagem interpretado</li>
 * <li>orderIndex: ordem de aparição nos créditos</li>
 * </ul>
 * 
 * @since 1.0
 */
public record CreditResponse(
        Long id, Long mediaId, Long personId, String role, String characterName, Short orderIndex) {
}