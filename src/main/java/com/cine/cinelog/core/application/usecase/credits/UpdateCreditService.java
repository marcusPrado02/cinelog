package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.UpdateCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UpdateCreditService implements UpdateCreditUseCase {
    private final CreditRepositoryPort repo;

    public UpdateCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    @Override
    public Credit execute(Long id, Credit credit) {
        Credit existing = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Credit not found: " + id));
        existing.setRole(credit.getRole());
        existing.setPersonId(credit.getPersonId());
        existing.setMediaId(credit.getMediaId());
        return repo.save(existing);
    }
}