package com.cine.cinelog.features.genres.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import com.cine.cinelog.features.genres.mapper.GenreMapper;
import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;
import com.cine.cinelog.features.genres.repository.GenreJpaRepository;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de Genre.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 * 
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Genre e GenreEntity.
 * </p>
 * 
 * @since 1.0
 * @see GenreRepositoryPort
 * @see GenreEntity
 * @see Genre
 */
@Repository
public class GenreRepositoryAdapter implements GenreRepositoryPort {

    private final GenreJpaRepository jpa;
    private final GenreMapper genreMapper;

    public GenreRepositoryAdapter(GenreJpaRepository jpa, GenreMapper genreMapper) {
        this.jpa = jpa;
        this.genreMapper = genreMapper;
    }

    @Override
    @Measured("cinelog.repository.genre.save")
    public Genre save(Genre genre) {
        var e = genreMapper.toEntity(genre);
        var s = jpa.save(e);
        return genreMapper.toDomain(s);
    }

    @Override
    @Measured("cinelog.repository.genre.findById")
    @AlertIfSlow(thresholdMs = 500)
    public Optional<Genre> findById(Long id) {
        return jpa.findById(id).map(genreMapper::toDomain);
    }

    @Override
    @Measured("cinelog.repository.genre.findAll")
    @AlertIfSlow(thresholdMs = 800)
    public PageResult<Genre> findAll(PageQuery query) {
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

        Page<GenreEntity> page = jpa.findAll(pageable);

        return PageResultMapper.from(page, genreMapper::toDomain);
    }

    @Override
    @Measured("cinelog.repository.genre.deleteById")
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Measured("cinelog.repository.genre.findByName")
    @AlertIfSlow(thresholdMs = 500)
    public Optional<Genre> findByName(String name) {
        return jpa.findByName(name).map(genreMapper::toDomain);
    }
}