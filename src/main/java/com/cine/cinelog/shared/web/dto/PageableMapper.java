package com.cine.cinelog.shared.web.dto;

import com.cine.cinelog.core.application.pagination.PageQuery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Mapper utilitário para converter Pageable (Spring) em PageQuery (domínio).
 * 
 * <p>
 * Converte objetos de paginação do Spring Data (camada web) para objetos
 * de paginação da camada de aplicação, isolando o domínio de dependências
 * do framework Spring.
 * 
 * <p>
 * Extrai informações de:
 * <ul>
 * <li>Número da página</li>
 * <li>Tamanho da página</li>
 * <li>Campo de ordenação (padrão: "id")</li>
 * <li>Direção de ordenação (padrão: "ASC")</li>
 * </ul>
 * 
 * @since 1.0
 */
public final class PageableMapper {

    private PageableMapper() {
    }

    /**
     * Converte Pageable do Spring em PageQuery do domínio.
     * 
     * @param pageable objeto de paginação do Spring Data
     * @return PageQuery para uso na camada de aplicação
     */
    public static PageQuery toPageQuery(Pageable pageable) {
        String sort = "id";
        String direction = "ASC";

        Sort s = pageable.getSort();
        if (s.isSorted()) {
            Sort.Order o = s.iterator().next();
            sort = o.getProperty();
            direction = o.getDirection().name();
        }

        return new PageQuery(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort,
                direction);
    }
}