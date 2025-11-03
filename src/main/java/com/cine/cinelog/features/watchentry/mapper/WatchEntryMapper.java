package com.cine.cinelog.features.watchentry.mapper;

import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryCreateRequest;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WatchEntryMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `WatchEntry`.
     * 
     * @param req
     * @return
     */
    WatchEntry toDomain(WatchEntryCreateRequest req);

    /**
     * Converte o modelo de domínio `WatchEntry` para o DTO de resposta.
     * 
     * @param domain
     * @return
     */
    WatchEntryResponse toResponse(WatchEntry domain);

    /**
     * Converte o modelo de domínio `WatchEntry` para a entidade JPA.
     * 
     * @param domain
     * @return
     */
    WatchEntryEntity toEntity(WatchEntry domain);

    /**
     * Converte `WatchEntryEntity` (persistência) para o modelo de domínio
     * `WatchEntry`.
     * 
     * @param entity
     * @return
     */
    WatchEntry toDomain(WatchEntryEntity entity);

}