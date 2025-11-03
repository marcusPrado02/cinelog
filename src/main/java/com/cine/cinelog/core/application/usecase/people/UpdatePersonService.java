package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.in.person.UpdatePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdatePersonService implements UpdatePersonUseCase {
    private final PersonRepositoryPort repo;

    public UpdatePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Person execute(Long id, Person person) {
        Person existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Person not found: " + id));

        existing.setName(person.getName());
        existing.setBirthDate(person.getBirthDate());
        existing.setPlaceOfBirth(person.getPlaceOfBirth());

        return repo.save(existing);
    }
}
