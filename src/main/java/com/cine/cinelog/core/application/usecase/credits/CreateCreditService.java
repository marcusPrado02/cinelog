package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.CreateCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para criação de créditos.
 */
@Service
@Transactional
public class CreateCreditService implements CreateCreditUseCase {
    private final CreditRepositoryPort repo;

    /**
     * Construtor do serviço de criação de créditos.
     *
     * @param repo O repositório de créditos.
     */
    public CreateCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a criação de um crédito.
     * 
     * @param credit O crédito a ser criado.
     */
    @Override
    public Credit execute(Credit credit) {
        return repo.save(credit);
    }
}