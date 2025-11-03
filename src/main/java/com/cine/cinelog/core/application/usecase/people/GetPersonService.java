package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.in.person.GetPersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetPersonService implements GetPersonUseCase {
    private final PersonRepositoryPort repo;

    public GetPersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Person execute(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));
    }
}