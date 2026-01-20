package com.cine.cinelog.features.watchentry.mapper;

import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryCreateRequest;
import com.cine.cinelog.features.watchentry.web.dto.WatchEntryResponse;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper responsável pela conversão entre WatchEntry e seus DTOs/Entidades.
 *
 * <p>
 * Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre:
 * <ul>
 * <li>Modelo de domínio (WatchEntry)</li>
 * <li>DTOs de requisição/resposta (WatchEntryCreateRequest,
 * WatchEntryResponse)</li>
 * <li>Entidade de persistência (WatchEntryEntity)</li>
 * </ul>
 *
 * @since 1.0
 * @see WatchEntry
 * @see WatchEntryEntity
 */
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

    /**
     * Reconstrói o estado após conversão de Entity para Domain.
     * Chamado automaticamente pelo MapStruct após toDomain(WatchEntryEntity).
     *
     * @param domain WatchEntry que teve estado reconstruído
     */
    @AfterMapping
    default void reconstructState(@MappingTarget WatchEntry domain) {
        if (domain != null) {
            domain.reconstructState();
        }
    }

    /**
     * Sincroniza statusType antes de converter Domain para Entity.
     * Chamado automaticamente pelo MapStruct após toEntity(WatchEntry).
     *
     * @param entity WatchEntryEntity que terá statusType sincronizado
     * @param domain WatchEntry fonte
     */
    @AfterMapping
    default void syncStatusType(@MappingTarget WatchEntryEntity entity, WatchEntry domain) {
        if (domain != null && entity != null) {
            domain.syncStatusType();
            entity.setStatusType(domain.getStatusType());
        }
    }

}
