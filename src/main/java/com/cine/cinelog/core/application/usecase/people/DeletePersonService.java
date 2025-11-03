package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.in.person.DeletePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeletePersonService implements DeletePersonUseCase {
    private final PersonRepositoryPort repo;

    public DeletePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}
