package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.in.genre.UpdateGenreUseCase;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateGenreService implements UpdateGenreUseCase {
    private final GenreRepositoryPort repo;

    public UpdateGenreService(GenreRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Genre execute(Long id, Genre genre) {
        var existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Genre not found: " + id));
        existing.setName(genre.getName());
        return repo.save(existing);
    }
}
