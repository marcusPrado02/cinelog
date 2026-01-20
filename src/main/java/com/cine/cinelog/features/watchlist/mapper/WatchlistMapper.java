package com.cine.cinelog.features.watchlist.mapper;

import com.cine.cinelog.core.domain.model.WatchlistItem;
import com.cine.cinelog.features.watchlist.persistence.WatchlistItemEntity;
import com.cine.cinelog.features.watchlist.web.dto.WatchlistResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper responsável pela conversão entre WatchlistItem e seus DTOs/Entidades.
 * 
 * <p>
 * Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre:
 * <ul>
 * <li>Modelo de domínio (WatchlistItem)</li>
 * <li>DTOs de requisição/resposta (WatchlistAddRequest, WatchlistResponse)</li>
 * <li>Entidade de persistência (WatchlistItemEntity)</li>
 * </ul>
 * 
 * @since 1.0
 * @see WatchlistItem
 * @see WatchlistItemEntity
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WatchlistMapper {

    /**
     * Converte o modelo de domínio `WatchlistItem` para o DTO de resposta.
     *
     * @param domain modelo de domínio
     * @return DTO para a API
     */
    @Mapping(target = "addedAt", expression = "java(domain.getAddedAt() != null ? domain.getAddedAt().toString() : null)")
    WatchlistResponse toResponse(WatchlistItem domain);

    /**
     * Converte o modelo de domínio `WatchlistItem` para a entidade JPA.
     *
     * @param domain modelo de domínio
     * @return entidade de persistência
     */
    WatchlistItemEntity toEntity(WatchlistItem domain);

    /**
     * Converte `WatchlistItemEntity` (persistência) para o modelo de domínio
     * `WatchlistItem`.
     *
     * @param entity entidade de persistência
     * @return modelo de domínio
     */
    WatchlistItem toDomain(WatchlistItemEntity entity);
}