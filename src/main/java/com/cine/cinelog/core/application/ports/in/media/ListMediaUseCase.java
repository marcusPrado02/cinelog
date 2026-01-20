package com.cine.cinelog.core.application.ports.in.media;

import java.util.List;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para listar mídias com paginação e filtros.
 *
 * Parâmetros:
 * - type: filtro por tipo de mídia (p.ex. FILME ou SÉRIE)
 * - q: termo livre para busca em títulos/descriptions
 * - page, size: paginação (0-based page index)
 *
 * Retorna uma lista de entidades `Media` que atendem aos critérios.
 */
public interface ListMediaUseCase {
    /**
     * Recupera uma lista de mídias existentes do sistema.
     *
     * @param pageQuery parâmetros de paginação
     * @return página de mídias encontradas
     */
    PageResult<Media> execute(PageQuery pageQuery);
}
