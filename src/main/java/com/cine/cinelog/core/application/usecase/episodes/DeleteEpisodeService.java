package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.in.episodes.DeleteEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteEpisodeService implements DeleteEpisodeUseCase {
    private final EpisodeRepositoryPort repo;

    public DeleteEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}