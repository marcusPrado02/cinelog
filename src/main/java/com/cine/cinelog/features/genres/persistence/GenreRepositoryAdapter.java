package com.cine.cinelog.features.genres.persistence;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.repository.GenreJpaRepository;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class GenreRepositoryAdapter implements GenreRepositoryPort {

    private final GenreJpaRepository jpa;
    private final GenreMapper genreMapper;

    public GenreRepositoryAdapter(GenreJpaRepository jpa, GenreMapper genreMapper) {
        this.jpa = jpa;
        this.genreMapper = genreMapper;
    }

    @Override
    public Genre save(Genre genre) {
        var e = genreMapper.toEntity(genre);
        var s = jpa.save(e);
        return genreMapper.toDomain(s);
    }

    @Override
    public Optional<Genre> findById(Long id) {
        return jpa.findById(id).map(genreMapper::toDomain);
    }

    @Override
    public List<Genre> findAll() {
        return jpa.findAll().stream().map(genreMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}