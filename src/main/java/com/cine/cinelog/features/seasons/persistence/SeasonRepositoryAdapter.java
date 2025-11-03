package com.cine.cinelog.features.seasons.persistence;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.features.seasons.mapper.SeasonMapper;
import com.cine.cinelog.features.seasons.repository.SeasonJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
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
    public List<Season> findAll() {
        return jpa.findAll().stream().map(seasonMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}