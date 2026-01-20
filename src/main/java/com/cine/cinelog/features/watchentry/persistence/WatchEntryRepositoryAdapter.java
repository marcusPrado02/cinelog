package com.cine.cinelog.features.watchentry.persistence;

import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.UserStats;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import com.cine.cinelog.features.watchentry.persistence.projection.UserStatsProjection;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;
import com.cine.cinelog.shared.observability.aop.Measured;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de WatchEntry.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 * 
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre WatchEntry e WatchEntryEntity.
 * </p>
 * 
 * @since 1.0
 * @see WatchEntryRepositoryPort
 * @see WatchEntryEntity
 * @see WatchEntry
 */
@Repository
public class WatchEntryRepositoryAdapter implements WatchEntryRepositoryPort {
    private final WatchEntryJpaRepository jpa;
    private final WatchEntryMapper WatchEntryMapper;

    public WatchEntryRepositoryAdapter(WatchEntryJpaRepository jpa, WatchEntryMapper WatchEntryMapper) {
        this.jpa = jpa;
        this.WatchEntryMapper = WatchEntryMapper;
    }

    @Override
    public WatchEntry save(WatchEntry entry) {
        var saved = jpa.save(WatchEntryMapper.toEntity(entry));
        return WatchEntryMapper.toDomain(saved);
    }

    @Override
    public Optional<WatchEntry> findById(Long id) {
        return jpa.findById(id).map(WatchEntryMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    @Measured("cinelog.repository.watchentry.listByUser")
    @AlertIfSlow(thresholdMs = 800)
    public PageResult<WatchEntry> listByUser(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable) {
        Page<WatchEntryEntity> page = jpa.search(userId, mediaId, episodeId, minRating, from, to, pageable);
        return PageResultMapper.from(page, WatchEntryMapper::toDomain);
    }

    @Override
    @Measured("cinelog.repository.watchentry.computeStatsForUser")
    @AlertIfSlow(thresholdMs = 1000)
    public UserStats computeStatsForUser(Long userId) {
        UserStatsProjection p = jpa.computeStatsForUser(userId);
        if (p == null) {
            // nunca assistiu nada: devolve stats vazias
            return new UserStats(userId, 0, 0, null, null, null);
        }
        return new UserStats(
                p.getUserId(),
                p.getTotalEntries() != null ? p.getTotalEntries() : 0L,
                p.getTotalRated() != null ? p.getTotalRated() : 0L,
                p.getAverageRating(),
                p.getFirstWatchDate(),
                p.getLastWatchDate());
    }

    @Override
    public boolean existsByMediaId(Long mediaId) {
        return jpa.existsByMediaId(mediaId);
    }

    @Override
    public boolean existsByUserMediaEpisodeAndDate(Long userId,
            Long mediaId,
            Long episodeId,
            LocalDate watchedAt) {
        return jpa.existsByUserIdAndMediaIdAndEpisodeIdAndWatchedAt(
                userId, mediaId, episodeId, watchedAt);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public boolean existsByEpisodeId(Long episodeId) {
        return jpa.existsByEpisodeId(episodeId);
    }

    @Override
    public long countEntriesByUserId(Long userId) {
        return jpa.countEntriesByUserId(userId);
    }

    @Override
    public long countRatedEntriesByUserId(Long userId) {
        return jpa.countRatedEntriesByUserId(userId);
    }

    @Override
    public Optional<Double> averageRatingByUserId(Long userId) {
        return jpa.averageRatingByUserId(userId);
    }

    @Override
    public LocalDate findFirstWatchDateByUserId(Long userId) {
        return jpa.findFirstWatchDateByUserId(userId);
    }

    @Override
    public LocalDate findLastWatchDateByUserId(Long userId) {
        return jpa.findLastWatchDateByUserId(userId);
    }
}