package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.in.episodes.ListEpisodesUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListEpisodesService implements ListEpisodesUseCase {
    private final EpisodeRepositoryPort repo;

    public ListEpisodesService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Episode> execute() {
        return repo.findAll();
    }
}