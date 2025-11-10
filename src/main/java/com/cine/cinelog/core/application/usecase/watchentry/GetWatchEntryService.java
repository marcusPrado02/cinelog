package com.cine.cinelog.core.application.usecase.watchentry;

import org.springframework.stereotype.Service;

import com.cine.cinelog.core.application.ports.in.watchentry.GetWatchEntryUseCase;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;

@Service
public class GetWatchEntryService implements GetWatchEntryUseCase {

    private final WatchEntryRepositoryPort repo;

    public GetWatchEntryService(WatchEntryRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public WatchEntry execute(Long id) {
        return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("WatchEntry não encontrado: " + id));
    }
}
