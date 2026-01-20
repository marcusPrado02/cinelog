package com.cine.cinelog.features.media.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;

/**
 * Mapper responsável pela conversão entre Media e seus DTOs/Entidades.
 *
 * <p>
 * Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre:
 * <ul>
 * <li>Modelo de domínio (Media)</li>
 * <li>DTOs de requisição/resposta (MediaCreateRequest, MediaUpdateRequest,
 * MediaResponse)</li>
 * <li>Entidade de persistência (MediaEntity)</li>
 * </ul>
 *
 * <p>
 * A configuração unmappedTargetPolicy = IGNORE permite que campos não mapeados
 * sejam ignorados silenciosamente. O campo `id` é ignorado ao criar/atualizar
 * via request
 * para evitar manipulação indevida de identificadores.
 *
 * @since 1.0
 * @see Media
 * @see MediaEntity
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface MediaMapper {

    /**
     * Converte DTO de criação para modelo de domínio.
     *
     * @param mediaCreateRequest DTO com dados de criação
     * @return modelo de domínio Media
     */
    Media toDomain(MediaCreateRequest mediaCreateRequest);

    /**
     * Converte o modelo de domínio `Media` para o DTO de resposta.
     *
     * @param media
     * @return
     */
    MediaResponse toResponse(Media media);

    /**
     * Converte o modelo de domínio `Media` para o DTO de criação.
     *
     * @param media
     * @return
     */
    MediaCreateRequest toCreateRequest(Media media);

    /**
     * Converte o modelo de domínio `Media` para a entidade JPA.
     *
     * @param media
     * @return
     */
    MediaEntity toEntity(Media media);

    /**
     * Converte `MediaEntity` (persistência) para o modelo de domínio `Media`.
     *
     * @param mediaEntity
     * @return
     */
    Media toDomain(MediaEntity mediaEntity);

    /**
     * Converte o modelo de domínio `Media` para o DTO de atualização.
     *
     * @param media
     * @return
     */
    MediaUpdateRequest toUpdateRequest(Media media);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Media`.
     *
     * @param mediaUpdateRequest
     * @return
     */
    Media toDomain(MediaUpdateRequest mediaUpdateRequest);

}
