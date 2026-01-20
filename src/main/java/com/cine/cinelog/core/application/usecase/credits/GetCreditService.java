package com.cine.cinelog.core.application.usecase.credits;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.credits.GetCreditUseCase;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Credit;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar um crédito específico por seu identificador.
 * 
 * <p>
 * Recupera informações sobre a participação de uma pessoa em uma mídia,
 * incluindo a função desempenhada.
 * 
 * <p>
 * Características:
 * <ul>
 * <li>Operação de leitura apenas ({@code readOnly = true})</li>
 * <li>Resultado cacheado com chave baseada no ID</li>
 * <li>Lança exceção se o crédito não for encontrado</li>
 * </ul>
 * 
 * @since 1.0
 * @see GetCreditUseCase
 * @see CreditRepositoryPort
 */
@Transactional(readOnly = true)
public class GetCreditService implements GetCreditUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetCreditService.class);

    private final CreditRepositoryPort repo;

    public GetCreditService(CreditRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca um crédito por seu identificador único.
     * 
     * @param id o identificador único do crédito
     * @return o crédito encontrado com pessoa, mídia e função
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se o
     *                         crédito não existir
     */
    @Override
    @Observed(name = "credit.get", contextualName = "get-credit-service")
    @Measured("cinelog.service.credit.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "creditById", key = "#id")
    public Credit execute(Long id) {
        log.debug("Buscando crédito. ID: {}", id);
        try {
            Credit credit = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Credit not found: " + id));
            log.debug("Crédito encontrado. ID: {}, Função: {}", id, credit.getRole());
            return credit;
        } catch (DomainException e) {
            log.warn("Crédito não encontrado. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar crédito. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}