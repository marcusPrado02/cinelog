package com.cine.cinelog.features.episodes.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import com.cine.cinelog.features.credits.persistence.entity.CreditEntity;
import com.cine.cinelog.features.episodes.mapper.EpisodeMapper;
import com.cine.cinelog.features.episodes.persistence.entity.EpisodeEntity;
import com.cine.cinelog.features.episodes.repository.EpisodeJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de Episode.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 * 
 * <p>Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Episode e EpisodeEntity.</p>
 * 
 * @since 1.0
 * @see EpisodeRepositoryPort
 * @see EpisodeEntity
 * @see Episode
 */
@Repository
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
    public PageResult<Episode> findAll(PageQuery query) {
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

        Page<EpisodeEntity> page = jpa.findAll(pageable);

        return PageResultMapper.from(page, episodeMapper::toDomain);
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
    public boolean existsById(Long id) {
        return jpa.existsById(id);
    }

    @Override
    public boolean existsBySeasonIdAndEpisodeNumber(Long seasonId, Integer episodeNumber) {
        return jpa.existsBySeasonIdAndEpisodeNumber(seasonId, episodeNumber);
    }

    @Override
    public boolean existsBySeasonId(Long seasonId) {
        return jpa.existsBySeasonId(seasonId);
    }
}