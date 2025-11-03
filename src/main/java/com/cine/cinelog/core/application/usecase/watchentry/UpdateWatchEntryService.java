package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.stereotype.Service;

@Service
public class UpdateWatchEntryService {
    private final WatchEntryRepositoryPort repo;

    public UpdateWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    public WatchEntry execute(Long id, WatchEntry patch) {
        var current = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("WatchEntry not found: " + id));
        // regra simples: substitui campos editáveis
        var merged = new WatchEntry(
                current.getId(),
                current.getUserId(),
                current.getMediaId() != null ? current.getMediaId() : patch.getMediaId(),
                current.getEpisodeId() != null ? current.getEpisodeId() : patch.getEpisodeId(),
                patch.getRating() != null ? patch.getRating() : current.getRating(),
                patch.getComment() != null ? patch.getComment() : current.getComment(),
                patch.getWatchedAt() != null ? patch.getWatchedAt() : current.getWatchedAt(),
                current.getCreatedAt(),
                current.getUpdatedAt());
        return repo.save(merged);
    }
}