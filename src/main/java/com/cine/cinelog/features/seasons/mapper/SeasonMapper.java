package com.cine.cinelog.features.seasons.mapper;

import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import com.cine.cinelog.features.seasons.web.dto.SeasonCreateRequest;
import com.cine.cinelog.features.seasons.web.dto.SeasonResponse;
import com.cine.cinelog.features.seasons.web.dto.SeasonUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper para conversão entre DTOs, entidades JPA e o modelo de domínio
 * `Season`.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SeasonMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `Season`.
     * 
     * @param request
     * @return
     */
    Season toDomain(SeasonCreateRequest request);

    /**
     * Converte o modelo de domínio `Season` para o DTO de resposta.
     * 
     * @param season
     * @return
     */
    SeasonResponse toResponse(Season season);

    /**
     * Converte o modelo de domínio `Season` para o DTO de atualização.
     * 
     * @param season
     * @return
     */
    SeasonUpdateRequest toUpdateRequest(Season season);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Season`.
     * 
     * @param request
     * @return
     */
    Season toDomain(SeasonUpdateRequest request);

    /**
     * Converte o modelo de domínio `Season` para a entidade JPA.
     * 
     * @param season
     * @return
     */
    SeasonEntity toEntity(Season season);

    /**
     * Converte `SeasonEntity` (persistência) para o modelo de domínio `Season`.
     * 
     * @param entity
     * @return
     */
    Season toDomain(SeasonEntity entity);

}
