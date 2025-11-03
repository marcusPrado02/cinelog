package com.cine.cinelog.features.episodes.mapper;

import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import com.cine.cinelog.features.episodes.web.dto.EpisodeCreateRequest;
import com.cine.cinelog.features.episodes.web.dto.EpisodeResponse;
import com.cine.cinelog.features.episodes.web.dto.EpisodeUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper para conversão entre diferentes representações de episódios.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EpisodeMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `Episode`.
     * 
     * @param request
     * @return
     */
    Episode toDomain(EpisodeCreateRequest request);

    /**
     * Converte o modelo de domínio `Episode` para o DTO de resposta.
     * 
     * @param episode
     * @return
     */
    EpisodeResponse toResponse(Episode episode);

    /**
     * Converte o modelo de domínio `Episode` para o DTO de atualização.
     * 
     * @param episode
     * @return
     */
    EpisodeUpdateRequest toUpdateRequest(Episode episode);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Episode`.
     * 
     * @param request
     * @return
     */
    Episode toDomain(EpisodeUpdateRequest request);

    /**
     * Converte o modelo de domínio `Episode` para a entidade JPA.
     * 
     * @param episode
     * @return
     */
    EpisodeEntity toEntity(Episode episode);

    /**
     * Converte `EpisodeEntity` (persistência) para o modelo de domínio `Episode`.
     * 
     * @param entity
     * @return
     */
    Episode toDomain(EpisodeEntity entity);

}
