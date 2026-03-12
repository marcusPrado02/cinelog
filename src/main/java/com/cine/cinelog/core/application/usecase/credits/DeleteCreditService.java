package com.cine.cinelog.core.application.usecase.credits;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.credits.DeleteCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.SecureOperation;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por excluir créditos (participações) do sistema.
 *
 * <p>
 * Remove a associação entre uma pessoa e uma mídia, eliminando
 * o registro da participação dessa pessoa na produção.
 *
 * @since 1.0
 * @see DeleteCreditUseCase
 * @see CreditRepositoryPort
 */
@Transactional
public class DeleteCreditService implements DeleteCreditUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeleteCreditService.class);

    private final CreditRepositoryPort repo;

    public DeleteCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de um crédito do sistema.
     *
     * @param id o identificador único do crédito a ser excluído
     */
    @Override
    @Observed(name = "credit.delete", contextualName = "delete-credit-service")
    @Measured("cinelog.service.credit.delete")
    @AuditableAction(module = "CREDIT", action = "DELETE", description = "Exclusão de crédito")
    @SecureOperation(module = "CREDIT", value = "CONTENT_ADMIN")
    @Caching(evict = {
            @CacheEvict(value = "creditsPage", allEntries = true),
            @CacheEvict(value = "creditById", key = "#id")
    })
    public void execute(Long id) {
        log.debug("Iniciando exclusão de crédito. ID: {}", id);
        try {
            repo.deleteById(id);
            log.info("Crédito excluído com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Erro ao excluir crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
