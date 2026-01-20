package com.cine.cinelog.core.application.usecase.credits;

import java.util.Map;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.credits.ListCreditsUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por listar créditos (participações) com paginação.
 * 
 * <p>
 * Retorna uma lista paginada de todas as participações de pessoas em mídias,
 * mostrando as associações entre pessoas, mídias e funções desempenhadas.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Suporte a paginação</li>
 * <li>Resultado cacheado para melhorar performance</li>
 * </ul>
 * 
 * @since 1.0
 * @see ListCreditsUseCase
 * @see CreditRepositoryPort
 */
@Transactional(readOnly = true)
public class ListCreditsService implements ListCreditsUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListCreditsService.class);

    private final CreditRepositoryPort repo;

    public ListCreditsService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todos os créditos do sistema de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação (página, tamanho, ordenação)
     * @return resultado paginado contendo os créditos
     */
    @Override
    @Observed(name = "credit.list", contextualName = "list-credits-service")
    @Measured("cinelog.service.credit.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "creditsPage", key = "#pageQuery.toString()")
    public PageResult<Credit> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de créditos. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));
        try {
            PageResult<Credit> result = repo.findAll(pageQuery);
            log.debug("Listagem de créditos concluída. Total encontrado: {}", result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar créditos. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
