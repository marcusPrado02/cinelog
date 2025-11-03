package com.cine.cinelog.features.media.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.features.media.persistence.entity.MediaEntity;
import com.cine.cinelog.features.media.web.dto.MediaCreateRequest;
import com.cine.cinelog.features.media.web.dto.MediaResponse;
import com.cine.cinelog.features.media.web.dto.MediaUpdateRequest;

/**
 * Mapper MapStruct para conversão entre DTOs REST e o modelo de domínio.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaMapper {

    /**
     * Mapper MapStruct para conversão entre DTOs REST e o modelo de domínio.
     * O campo `id` é ignorado ao criar/updating via request para evitar
     * sobrescrever o identificador gerado.
     * 
     * @param mediaCreateRequest
     * @return
     */
    @Mapping(target = "id", ignore = true)
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
