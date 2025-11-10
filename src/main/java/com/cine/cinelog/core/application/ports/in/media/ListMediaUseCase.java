package com.cine.cinelog.core.application.ports.in.media;

import java.util.List;

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
     * @param type O tipo de mídia a ser filtrado.
     * @param q    O termo de busca para títulos ou descrições.
     * @param page O índice da página (0-based).
     * @param size O número de itens por página.
     * @return Uma lista de mídias que atendem aos critérios.
     */
    List<Media> execute(MediaType type, String q, int page, int size);
}
