package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.GetGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetGenreService implements GetGenreUseCase {
    private final GenreRepositoryPort repo;

    public GetGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Genre execute(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + id));
    }
}