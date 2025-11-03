package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.in.person.ListPeopleUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListPeopleService implements ListPeopleUseCase {
    private final PersonRepositoryPort repo;

    public ListPeopleService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Person> execute() {
        return repo.findAll();
    }
}
