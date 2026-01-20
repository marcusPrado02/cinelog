package com.cine.cinelog.features.genres.mapper;

import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;
import com.cine.cinelog.features.genres.web.dto.GenreCreateRequest;
import com.cine.cinelog.features.genres.web.dto.GenreResponse;
import com.cine.cinelog.features.genres.web.dto.GenreUpdateRequest;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/**
 * Mapper responsável pela conversão entre Genre e seus DTOs/Entidades.
 * 
 * <p>
 * Utiliza MapStruct para gerar implementações automatizadas das conversões
 * entre:
 * <ul>
 * <li>Modelo de domínio (Genre)</li>
 * <li>DTOs de requisição/resposta (GenreCreateRequest, GenreUpdateRequest,
 * GenreResponse)</li>
 * <li>Entidade de persistência (GenreEntity)</li>
 * </ul>
 * 
 * <p>
 * A configuração unmappedTargetPolicy = IGNORE permite que campos não mapeados
 * sejam ignorados silenciosamente, facilitando conversões parciais.
 * 
 * @since 1.0
 * @see Genre
 * @see GenreEntity
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GenreMapper {

    /**
     * Converte o DTO de criação para o modelo de domínio `Genre`.
     * 
     * @param request
     * @return
     */
    Genre toDomain(GenreCreateRequest request);

    /**
     * Converte o modelo de domínio `Genre` para o DTO de resposta.
     * 
     * @param genre
     * @return
     */
    GenreResponse toResponse(Genre genre);

    /**
     * Converte o modelo de domínio `Genre` para o DTO de atualização.
     * 
     * @param genre
     * @return
     */
    GenreUpdateRequest toUpdateRequest(Genre genre);

    /**
     * Converte o DTO de atualização para o modelo de domínio `Genre`.
     * 
     * @param request
     * @return
     */
    Genre toDomain(GenreUpdateRequest request);

    /**
     * Converte o modelo de domínio `Genre` para a entidade JPA.
     * 
     * @param genre
     * @return
     */
    GenreEntity toEntity(Genre genre);

    /**
     * Converte `GenreEntity` (persistência) para o modelo de domínio `Genre`.
     * 
     * @param genreEntity
     * @return
     */
    Genre toDomain(GenreEntity genreEntity);

}
