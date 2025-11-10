package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.in.watchentry.DeleteWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import org.springframework.stereotype.Service;

@Service
public class DeleteWatchEntryService implements DeleteWatchEntryUseCase {
    private final WatchEntryRepositoryPort repo;

    public DeleteWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    public void execute(Long id) {
        repo.deleteById(id);
    }
}
