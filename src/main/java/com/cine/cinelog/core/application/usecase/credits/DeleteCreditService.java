package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.DeleteCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeleteCreditService implements DeleteCreditUseCase {
    private final CreditRepositoryPort repo;

    public DeleteCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}