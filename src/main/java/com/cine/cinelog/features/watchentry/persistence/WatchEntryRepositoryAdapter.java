package com.cine.cinelog.features.watchentry.persistence;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.features.watchentry.mapper.WatchEntryMapper;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

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
    public Page<WatchEntry> listByUser(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable) {
        return jpa.search(userId, mediaId, episodeId, minRating, from, to, pageable)
                .map(WatchEntryMapper::toDomain);
    }
}