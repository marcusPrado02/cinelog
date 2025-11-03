package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.in.season.UpdateSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateSeasonService implements UpdateSeasonUseCase {
    private final SeasonRepositoryPort repo;

    public UpdateSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Season execute(Long id, Season season) {
        Season existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + id));

        existing.setSeasonNumber(season.getSeasonNumber());
        existing.setName(season.getName());
        existing.setMediaId(season.getMediaId());

        return repo.save(existing);
    }
}