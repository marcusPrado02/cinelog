package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Genre;
import java.util.List;
import java.util.Optional;

public interface GenreRepositoryPort {
    Genre save(Genre genre);

    Optional<Genre> findById(Long id);

    List<Genre> findAll();

    void deleteById(Long id);
}