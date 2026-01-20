package com.cine.cinelog.features.watchentry.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

/**
 * DTO de requisição para registrar visualização de mídia ou episódio.
 * 
 * <p>
 * Registra que um usuário assistiu uma mídia (filme/série completa) ou episódio
 * específico:
 * <ul>
 * <li>userId: ID do usuário (obrigatório)</li>
 * <li>mediaId: ID da mídia assistida (opcional - usar quando for filme ou série
 * completa)</li>
 * <li>episodeId: ID do episódio assistido (opcional - usar quando for episódio
 * específico)</li>
 * <li>rating: avaliação de 0 a 10 (opcional)</li>
 * <li>comment: comentário sobre a visualização (opcional, máx. 10.000
 * caracteres)</li>
 * <li>watchedAt: data em que assistiu (opcional, usa data atual se não
 * informado)</li>
 * </ul>
 * 
 * <p>
 * Nota: Deve fornecer mediaId OU episodeId, mas não ambos.
 * 
 * @since 1.0
 */
@Schema(name = "WatchEntryCreateRequest")
public record WatchEntryCreateRequest(
                @NotNull Long userId,
                Long mediaId,
                Long episodeId,
                @Min(0) @Max(10) Integer rating,
                @Size(max = 10_000) String comment,
                LocalDate watchedAt) {
}
