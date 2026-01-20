package com.cine.cinelog.core.application.usecase.seasons;

import java.util.Map;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.season.ListSeasonsUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por listar temporadas com paginação.
 * 
 * <p>
 * Retorna uma lista paginada de temporadas cadastradas no sistema.
 * 
 * @since 1.0
 * @see ListSeasonsUseCase
 * @see SeasonRepositoryPort
 */
@Transactional(readOnly = true)
public class ListSeasonsService implements ListSeasonsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListSeasonsService.class);

    private final SeasonRepositoryPort repo;

    public ListSeasonsService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todas as temporadas do sistema de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação
     * @return resultado paginado contendo as temporadas
     */
    @Override
    @Observed(name = "season.list", contextualName = "list-seasons-service")
    @Measured("cinelog.service.season.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "seasonsPage", key = "#pageQuery.toString()")
    public PageResult<Season> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de temporadas. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));
        try {
            PageResult<Season> result = repo.findAll(pageQuery);
            log.debug("Listagem de temporadas concluída. Total encontrado: {}", result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar temporadas. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
