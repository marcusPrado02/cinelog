package com.cine.cinelog.core.application.usecase.people;

import java.util.Map;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.person.ListPeopleUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por listar pessoas (profissionais do cinema/TV) com
 * paginação.
 * 
 * <p>
 * Retorna uma lista paginada de todas as pessoas cadastradas no sistema.
 * 
 * @since 1.0
 * @see ListPeopleUseCase
 * @see PersonRepositoryPort
 */
@Transactional(readOnly = true)
public class ListPeopleService implements ListPeopleUseCase {
    private static final Logger log = LoggerFactory.getLogger(ListPeopleService.class);

    private final PersonRepositoryPort repo;

    public ListPeopleService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Lista todas as pessoas do sistema de forma paginada.
     * 
     * @param pageQuery os parâmetros de paginação
     * @return resultado paginado contendo as pessoas
     */
    @Override
    @Observed(name = "person.list", contextualName = "list-people-service")
    @Measured("cinelog.service.person.list")
    @AlertIfSlow(thresholdMs = 800)
    @Cacheable(value = "peoplePage", key = "#pageQuery.toString()")
    public PageResult<Person> execute(PageQuery pageQuery) {
        log.debug("Iniciando listagem de pessoas. Parâmetros: {}",
                Map.of("page", pageQuery.page(), "size", pageQuery.size()));
        try {
            PageResult<Person> result = repo.findAll(pageQuery);
            log.debug("Listagem de pessoas concluída. Total encontrado: {}", result.totalElements());
            return result;
        } catch (Exception e) {
            log.error("Erro inesperado ao listar pessoas. Erro: {}", e.getMessage(), e);
            throw e;
        }
    }
}
