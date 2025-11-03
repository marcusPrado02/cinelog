package com.cine.cinelog.features.episodes.persistence;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.repository.EpisodeJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class EpisodeRepositoryAdapter implements EpisodeRepositoryPort {

    private final EpisodeJpaRepository jpa;
    private final EpisodeMapper episodeMapper;

    public EpisodeRepositoryAdapter(EpisodeJpaRepository jpa, EpisodeMapper episodeMapper) {
        this.jpa = jpa;
        this.episodeMapper = episodeMapper;
    }

    @Override
    public Episode save(Episode episode) {
        var e = episodeMapper.toEntity(episode);
        var s = jpa.save(e);
        return episodeMapper.toDomain(s);
    }

    @Override
    public Optional<Episode> findById(Long id) {
        return jpa.findById(id).map(episodeMapper::toDomain);
    }

    @Override
    public List<Episode> findAll() {
        return jpa.findAll().stream().map(episodeMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}