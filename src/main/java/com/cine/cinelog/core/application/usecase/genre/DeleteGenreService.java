package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.DeleteGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteGenreService implements DeleteGenreUseCase {
    private final GenreRepositoryPort repo;

    public DeleteGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}