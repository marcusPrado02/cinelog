package com.cine.cinelog.features.seasons.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.persistence.entity.SeasonEntity;
import com.cine.cinelog.features.seasons.repository.SeasonJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de Season.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 * 
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Season e SeasonEntity.
 * </p>
 * 
 * @since 1.0
 * @see SeasonRepositoryPort
 * @see SeasonEntity
 * @see Season
 */
@Repository
public class SeasonRepositoryAdapter implements SeasonRepositoryPort {

    private final SeasonJpaRepository jpa;
    private final SeasonMapper seasonMapper;

    public SeasonRepositoryAdapter(SeasonJpaRepository jpa, SeasonMapper seasonMapper) {
        this.jpa = jpa;
        this.seasonMapper = seasonMapper;
    }

    @Override
    public Season save(Season season) {
        var e = seasonMapper.toEntity(season);
        var s = jpa.save(e);
        return seasonMapper.toDomain(s);
    }

    @Override
    public Optional<Season> findById(Long id) {
        return jpa.findById(id).map(seasonMapper::toDomain);
    }

    @Override
    public PageResult<Season> findAll(PageQuery query) {
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

        Page<SeasonEntity> page = jpa.findAll(pageable);

        return PageResultMapper.from(page, seasonMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsByMediaId(Long mediaId) {
        return jpa.existsByMediaId(mediaId);
    }

    @Override
    public boolean existsByMediaIdAndSeasonNumber(Long mediaId, Integer seasonNumber) {
        return jpa.existsByMediaIdAndSeasonNumber(mediaId, seasonNumber);
    }

    @Override
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }
}