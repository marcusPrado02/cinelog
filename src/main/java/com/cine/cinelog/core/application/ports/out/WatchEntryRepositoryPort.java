package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface WatchEntryRepositoryPort {
    WatchEntry save(WatchEntry entry);

    Optional<WatchEntry> findById(Long id);

    void deleteById(Long id);

    Page<WatchEntry> listByUser(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable);
}