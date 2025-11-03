package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.in.person.CreatePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreatePersonService implements CreatePersonUseCase {
    private final PersonRepositoryPort repo;

    public CreatePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Person execute(Person person) {
        return repo.save(person);
    }
}