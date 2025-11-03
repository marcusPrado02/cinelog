package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.GetCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCreditService implements GetCreditUseCase {
    private final CreditRepositoryPort repo;

    public GetCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Credit execute(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found: " + id));
    }
}