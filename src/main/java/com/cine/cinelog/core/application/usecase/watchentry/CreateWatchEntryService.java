package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.in.watchentry.CreateWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.springframework.stereotype.Service;

@Service
public class CreateWatchEntryService implements CreateWatchEntryUseCase {
    private final WatchEntryRepositoryPort repo;

    public CreateWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    public WatchEntry execute(WatchEntry entry) {
        return repo.save(entry);
    }
}
