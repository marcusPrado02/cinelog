package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.in.episodes.UpdateEpisodeUseCase;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateEpisodeService implements UpdateEpisodeUseCase {
    private final EpisodeRepositoryPort repo;

    public UpdateEpisodeService(EpisodeRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Episode execute(Long id, Episode episode) {
        Episode existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Episode not found: " + id));

        existing.setName(episode.getName());
        existing.setEpisodeNumber(episode.getEpisodeNumber());
        existing.setSeasonId(episode.getSeasonId());
        existing.setAirDate(episode.getAirDate());

        return repo.save(existing);
    }
}