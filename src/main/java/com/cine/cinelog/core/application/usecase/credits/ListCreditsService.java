package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.ListCreditsUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListCreditsService implements ListCreditsUseCase {
    private final CreditRepositoryPort repo;

    public ListCreditsService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public List<Credit> execute() {
        return repo.findAll();
    }
}
