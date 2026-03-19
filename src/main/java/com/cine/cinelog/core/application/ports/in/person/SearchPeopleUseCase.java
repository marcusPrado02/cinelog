package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Person;

/**
 * Caso de uso para busca de pessoas por nome.
 */
public interface SearchPeopleUseCase {
    /**
     * Pesquisa pessoas pelo nome (case-insensitive, substring).
     *
     * @param name      termo de busca
     * @param pageQuery parâmetros de paginação
     * @return página de pessoas encontradas
     */
    PageResult<Person> execute(String name, PageQuery pageQuery);
}
