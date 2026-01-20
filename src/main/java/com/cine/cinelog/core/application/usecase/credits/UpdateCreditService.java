package com.cine.cinelog.core.application.usecase.credits;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.credits.UpdateCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por atualizar créditos existentes.
 * 
 * <p>
 * Permite alterar a função (role) de uma pessoa em uma mídia,
 * ou modificar qual pessoa ou mídia está associada ao crédito.
 * 
 * @since 1.0
 * @see UpdateCreditUseCase
 * @see CreditRepositoryPort
 */
@Transactional
public class UpdateCreditService implements UpdateCreditUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdateCreditService.class);

    private final CreditRepositoryPort repo;

    public UpdateCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a atualização de um crédito existente.
     * 
     * @param id     o identificador único do crédito a ser atualizado
     * @param credit os novos dados (função, pessoa ou mídia)
     * @return o crédito atualizado e persistido
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         crédito não existir
     */
    @Override
    @Observed(name = "credit.update", contextualName = "update-credit-service")
    @Measured("cinelog.service.credit.update")
    @AuditableAction(module = "CREDIT", action = "UPDATE", description = "Atualização de crédito")
    public Credit execute(Long id, Credit credit) {
        log.debug("Iniciando atualização de crédito. ID: {}", id);

        try {
            log.debug("Buscando crédito existente. ID: {}", id);
            Credit existing = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Credit not found: " + id));

            log.debug("Crédito encontrado. Aplicando atualizações");
            existing.setRole(credit.getRole());
            existing.setPersonId(credit.getPersonId());
            existing.setMediaId(credit.getMediaId());

            Credit saved = repo.save(existing);
            log.info("Crédito atualizado com sucesso. ID: {}, Função: {}", id, saved.getRole());
            return saved;
        } catch (DomainException e) {
            log.warn("Crédito não encontrado. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}