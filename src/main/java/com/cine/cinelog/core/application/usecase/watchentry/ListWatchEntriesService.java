package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.in.watchentry.ListWatchEntriesUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class ListWatchEntriesService implements ListWatchEntriesUseCase {
    private final WatchEntryRepositoryPort repo;

    public ListWatchEntriesService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    public Page<WatchEntry> execute(Long userId, Long mediaId, Long episodeId, Integer minRating,
            LocalDate from, LocalDate to, Pageable pageable) {
        if (userId == null)
            throw new IllegalArgumentException("userId is required");
        return repo.listByUser(userId, mediaId, episodeId, minRating, from, to, pageable);
    }
}
