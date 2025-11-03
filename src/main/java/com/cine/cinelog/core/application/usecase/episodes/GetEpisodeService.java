package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.in.episodes.GetEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetEpisodeService implements GetEpisodeUseCase {
    private final EpisodeRepositoryPort repo;

    public GetEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Episode execute(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + id));
    }
}