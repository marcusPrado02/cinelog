package com.cine.cinelog.core.application.usecase.episodes;

import java.util.Map;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.episodes.ListEpisodesUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por listar episódios com paginação.
 * 
 * <p>
 * Retorna uma lista paginada de episódios cadastrados no sistema,
 * permitindo navegação eficiente através dos episódios das temporadas.
 * 
 * @since 1.0
 * @see ListEpisodesUseCase
 * @see EpisodeRepositoryPort
 */
@Transactional(readOnly = true)
public class ListEpisodesService implements ListEpisodesUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListEpisodesService.class);

    private final EpisodeRepositoryPort repo;

    public ListEpisodesService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todos os episódios do sistema de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação
     * @return resultado paginado contendo os episódios
     */
    @Override
    @Observed(name = "episode.list", contextualName = "list-episodes-service")
    @Measured("cinelog.service.episode.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "episodesPage", key = "#pageQuery.toString()")
    public PageResult<Episode> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de episódios. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));
        try {
            PageResult<Episode> result = repo.findAll(pageQuery);
            log.debug("Listagem de episódios concluída. Total encontrado: {}", result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar episódios. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}