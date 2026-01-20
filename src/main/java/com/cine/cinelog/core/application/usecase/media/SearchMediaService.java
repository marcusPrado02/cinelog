package com.cine.cinelog.core.application.usecase.media;

import org.springframework.transaction.annotation.Transactional;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.media.SearchMediaUseCase;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.query.MediaSearchCriteria;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

/**
 * Serviço responsável por buscar mídias com filtros avançados e paginação.
 * 
 * <p>
 * Este caso de uso permite realizar buscas sofisticadas de mídias utilizando
 * diversos critérios de filtragem simultaneamente:
 * <ul>
 * <li>Título (busca parcial/like)</li>
 * <li>Tipo de mídia (filme ou série)</li>
 * <li>Gêneros</li>
 * <li>Ano de lançamento (intervalo)</li>
 * <li>Classificação indicativa</li>
 * <li>Outros critérios definidos em {@link MediaSearchCriteria}</li>
 * </ul>
 * 
 * <p>
 * Os resultados são retornados de forma paginada, permitindo navegação
 * eficiente através de grandes conjuntos de resultados.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Suporte a múltiplos filtros combinados</li>
 * <li>Paginação e ordenação configuráveis</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link SearchMediaUseCase} e utilizando a porta de saída
 * {@link MediaRepositoryPort}.
 * 
 * @since 1.0
 * @see SearchMediaUseCase
 * @see MediaSearchCriteria
 * @see MediaRepositoryPort
 */
@Transactional(readOnly = true)
public class SearchMediaService implements SearchMediaUseCase {

    private final MediaRepositoryPort repository;

    public SearchMediaService(MediaRepositoryPort repository) {
        this.repository = repository;
    }

    /**
     * Executa uma busca de mídias com base nos critérios fornecidos.
     * 
     * <p>
     * Os critérios de busca são combinados com parâmetros de paginação
     * para retornar um subconjunto ordenado dos resultados.
     * 
     * @param criteria  os critérios de filtragem (título, tipo, gêneros, ano, etc.)
     * @param pageQuery os parâmetros de paginação (página, tamanho, ordenação)
     * @return resultado paginado contendo as mídias que atendem aos critérios
     */
    @Override
    @Measured("cinelog.usecase.search_media")
    @AlertIfSlow(thresholdMs = 800, metricName = "cinelog.slow_search_query")
    public PageResult<Media> execute(MediaSearchCriteria criteria, PageQuery pageQuery) {
        criteria.setPage(pageQuery.page());
        criteria.setSize(pageQuery.size());

        return repository.search(criteria, pageQuery);
    }
}