package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.in.season.DeleteSeasonUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteSeasonService implements DeleteSeasonUseCase {
    private final SeasonRepositoryPort repo;

    public DeleteSeasonService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}