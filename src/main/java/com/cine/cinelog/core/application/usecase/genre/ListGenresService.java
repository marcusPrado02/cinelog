package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.genre.ListGenresUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serviço responsável por listar gêneros de mídia com paginação.
 * 
 * <p>
 * Este caso de uso retorna uma lista paginada de todos os gêneros
 * cadastrados, utilizados para categorizar mídias (filmes e séries).
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Suporte a paginação através de {@link PageQuery}</li>
 * <li>Resultado cacheado para melhorar performance</li>
 * </ul>
 * 
 * <p>
 * Este serviço faz parte da arquitetura hexagonal, implementando a porta de
 * entrada
 * {@link ListGenresUseCase} e utilizando a porta de saída
 * {@link GenreRepositoryPort}.
 * 
 * @since 1.0
 * @see ListGenresUseCase
 * @see GenreRepositoryPort
 */
@Transactional(readOnly = true)
public class ListGenresService implements ListGenresUseCase {

    private static final Logger log = LoggerFactory.getLogger(ListGenresService.class);

    private final GenreRepositoryPort repo;

    public ListGenresService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todos os gêneros do sistema de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação (página, tamanho, ordenação)
     * @return resultado paginado contendo os gêneros
     */
    @Observed(name = "genre.list", contextualName = "list-genres-service")
    @Measured("cinelog.service.genre.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "genresPage", key = "#pageQuery.toString()")
    @Override
    public PageResult<Genre> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de gêneros. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));

        try {
            PageResult<Genre> result = repo.findAll(pageQuery);
            log.debug("Listagem de gêneros concluída. Total encontrado: {}", result.totalElements());
            return result;

        } catch (Exception e) {
            log.error("Erro inesperado ao listar gêneros. Parâmetros: {}, Erro: {}",
                    Map.of("page", pageQuery.page(), "size", pageQuery.size()), e.getMessage(), e);
            throw e;
        }
    }
}