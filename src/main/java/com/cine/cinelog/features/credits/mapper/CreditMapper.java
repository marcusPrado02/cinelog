package com.cine.cinelog.features.credits.mapper;

import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import com.cine.cinelog.features.credits.web.dto.CreditCreateRequest;
import com.cine.cinelog.features.credits.web.dto.CreditResponse;
import com.cine.cinelog.features.credits.web.dto.CreditUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper para converter entre o modelo de domínio `Credit`, entidades JPA e
 * DTOs.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CreditMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `Credit`.
     * 
     * @param request
     * @return
     */
    Credit toDomain(CreditCreateRequest request);

    /**
     * Converte o modelo de domínio `Credit` para o DTO de resposta.
     * 
     * @param credit
     * @return
     */
    CreditResponse toResponse(Credit credit);

    /**
     * Converte o modelo de domínio `Credit` para o DTO de atualização.
     * 
     * @param credit
     * @return
     */
    CreditUpdateRequest toUpdateRequest(Credit credit);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Credit`.
     * 
     * @param request
     * @return
     */
    Credit toDomain(CreditUpdateRequest request);

    /**
     * Converte o modelo de domínio `Credit` para a entidade JPA.
     * 
     * @param credit
     * @return
     */
    CreditEntity toEntity(Credit credit);

    /**
     * Converte `CreditEntity` (persistência) para o modelo de domínio `Credit`.
     * 
     * @param entity
     * @return
     */
    Credit toDomain(CreditEntity entity);

}
