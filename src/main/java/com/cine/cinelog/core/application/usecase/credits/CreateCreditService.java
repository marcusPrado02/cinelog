package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.CreateCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CreateCreditService implements CreateCreditUseCase {
    private final CreditRepositoryPort repo;

    public CreateCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Credit execute(Credit credit) {
        return repo.save(credit);
    }
}