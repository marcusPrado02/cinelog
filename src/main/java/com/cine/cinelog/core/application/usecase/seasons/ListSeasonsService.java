package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.in.season.ListSeasonsUseCase;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListSeasonsService implements ListSeasonsUseCase {
    private final SeasonRepositoryPort repo;

    public ListSeasonsService(SeasonRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Season> execute() {
        return repo.findAll();
    }
}
