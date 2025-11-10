package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.ports.in.credits.DeleteCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço para exclusão de créditos.
 */
@Service
@Transactional
public class DeleteCreditService implements DeleteCreditUseCase {
    private final CreditRepositoryPort repo;

    /**
     * Construtor do serviço de exclusão de créditos.
     *
     * @param repo O repositório de créditos.
     */
    public DeleteCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de um crédito.
     * 
     * @param id O ID do crédito a ser excluído.
     */
    @Override
    public void execute(Long id) {
        repo.deleteById(id);
    }
}