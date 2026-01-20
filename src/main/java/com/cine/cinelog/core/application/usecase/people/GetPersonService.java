package com.cine.cinelog.core.application.usecase.people;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.person.GetPersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.shared.observability.aop.Measured;
import com.cine.cinelog.shared.observability.aop.AlertIfSlow;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por buscar uma pessoa específica por seu identificador.
 * 
 * <p>
 * Recupera os dados completos de um profissional do cinema/TV,
 * incluindo nome, data e local de nascimento.
 * 
 * @since 1.0
 * @see GetPersonUseCase
 * @see PersonRepositoryPort
 */
@Transactional(readOnly = true)
public class GetPersonService implements GetPersonUseCase {
    private static final Logger log = LoggerFactory.getLogger(GetPersonService.class);

    private final PersonRepositoryPort repo;

    public GetPersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Busca uma pessoa por seu identificador único.
     * 
     * @param id o identificador único da pessoa
     * @return a pessoa encontrada
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se a
     *                         pessoa não existir
     */
    @Override
    @Observed(name = "person.get", contextualName = "get-person-service")
    @Measured("cinelog.service.person.get")
    @AlertIfSlow(thresholdMs = 500)
    @Cacheable(value = "personById", key = "#id")
    public Person execute(Long id) {
        log.debug("Buscando pessoa. ID: {}", id);
        try {
            Person person = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Person not found: " + id));
            log.debug("Pessoa encontrada. ID: {}, Nome: {}", id, person.getName());
            return person;
        } catch (DomainException e) {
            log.warn("Pessoa não encontrada. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao buscar pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}