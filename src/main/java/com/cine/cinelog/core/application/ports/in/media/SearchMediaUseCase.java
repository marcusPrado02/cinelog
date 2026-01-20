package com.cine.cinelog.core.application.ports.in.media;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.model.Media;

/**
 * Caso de uso para busca avançada de mídias com múltiplos critérios.
 * 
 * <p>
 * Permite pesquisar mídias utilizando diversos filtros combinados:
 * <ul>
 * <li>Tipo de mídia (filme ou série)</li>
 * <li>Gêneros</li>
 * <li>Intervalo de anos</li>
 * <li>Classificação etária</li>
 * <li>Texto livre em título e descrição</li>
 * </ul>
 * 
 * <p>
 * Os resultados são paginados para melhor performance em grandes conjuntos de
 * dados.
 * 
 * @since 1.0
 * @see MediaSearchCriteria
 * @see PageQuery
 */
public interface SearchMediaUseCase {

    /**
     * Executa uma busca avançada de mídias com os critérios especificados.
     * 
     * @param criteria  os critérios de busca (tipo, gêneros, ano, etc.)
     * @param pageQuery parâmetros de paginação
     * @return resultado paginado com as mídias encontradas
     */
    PageResult<Media> execute(MediaSearchCriteria criteria, PageQuery pageQuery);
}