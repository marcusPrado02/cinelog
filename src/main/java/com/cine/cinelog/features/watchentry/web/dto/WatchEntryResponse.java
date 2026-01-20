package com.cine.cinelog.features.watchentry.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de resposta contendo informações completas de um registro de
 * visualização.
 * 
 * <p>
 * Retorna todos os dados do registro de visualização incluindo informações de
 * auditoria:
 * <ul>
 * <li>id: identificador único do registro</li>
 * <li>userId: ID do usuário que assistiu</li>
 * <li>mediaId: ID da mídia assistida (nulo se for episódio)</li>
 * <li>episodeId: ID do episódio assistido (nulo se for mídia completa)</li>
 * <li>rating: avaliação de 0 a 10</li>
 * <li>comment: comentário sobre a visualização</li>
 * <li>watchedAt: data em que assistiu</li>
 * <li>createdAt: data/hora de criação do registro</li>
 * <li>updatedAt: data/hora da última atualização</li>
 * <li>createdBy: identificador de quem criou o registro</li>
 * <li>updatedBy: identificador de quem fez a última atualização</li>
 * <li>version: versão do registro para controle de concorrência otimista</li>
 * </ul>
 * 
 * @since 1.0
 */
@Schema(name = "WatchEntryResponse")
public record WatchEntryResponse(
        Long id,
        Long userId,
        Long mediaId,
        Long episodeId,
        Integer rating,
        String comment,
        LocalDate watchedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long createdBy,
        Long updatedBy,
        Long version) {
}