package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.in.season.GetSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetSeasonService implements GetSeasonUseCase {
    private final SeasonRepositoryPort repo;

    public GetSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Season execute(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Season not found: " + id));
    }
}