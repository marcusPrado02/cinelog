package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.CreateGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateGenreService implements CreateGenreUseCase {
    private final GenreRepositoryPort repo;

    public CreateGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Genre execute(Genre genre) {
        return repo.save(genre);
    }
}