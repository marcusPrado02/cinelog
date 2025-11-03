package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.ListGenresUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListGenresService implements ListGenresUseCase {
    private final GenreRepositoryPort repo;

    public ListGenresService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Genre> execute() {
        return repo.findAll();
    }
}