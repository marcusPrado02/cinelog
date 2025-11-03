package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.in.season.CreateSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateSeasonService implements CreateSeasonUseCase {

    private final SeasonRepositoryPort repo;

    public CreateSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Season execute(Season season) {
        return repo.save(season);
    }
}
