package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.in.episodes.CreateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateEpisodeService implements CreateEpisodeUseCase {
    private final EpisodeRepositoryPort repo;

    public CreateEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Episode execute(Episode episode) {
        return repo.save(episode);
    }
}
