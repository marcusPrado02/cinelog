package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.in.person.SearchPeopleUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;

import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela busca de pessoas por nome.
 *
 * <p>
 * Delega a pesquisa para o repositório usando busca por substring
 * case-insensitive com paginação.
 * </p>
 *
 * @since 1.0
 * @see SearchPeopleUseCase
 * @see PersonRepositoryPort
 */
@Transactional(readOnly = true)
public class SearchPeopleService implements SearchPeopleUseCase {

    private final PersonRepositoryPort repo;

    public SearchPeopleService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public PageResult<Person> execute(String name, PageQuery pageQuery) {
        return repo.searchByName(name, pageQuery);
    }
}
