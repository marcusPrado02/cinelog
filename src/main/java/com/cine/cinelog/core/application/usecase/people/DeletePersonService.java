package com.cine.cinelog.core.application.usecase.people;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.person.DeletePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
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
 * Serviço responsável por excluir pessoas do sistema.
 *
 * <p>
 * A exclusão pode falhar se houver créditos (participações em mídias)
 * associados à pessoa,
 * garantindo integridade referencial.
 *
 * @since 1.0
 * @see DeletePersonUseCase
 * @see PersonRepositoryPort
 */
@Transactional
public class DeletePersonService implements DeletePersonUseCase {
    private static final Logger log = LoggerFactory.getLogger(DeletePersonService.class);

    private final PersonRepositoryPort repo;

    public DeletePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a exclusão de uma pessoa do sistema.
     *
     * @param id o identificador único da pessoa a ser excluída
     */
    @Override
    @Observed(name = "person.delete", contextualName = "delete-person-service")
    @Measured("cinelog.service.person.delete")
    @AuditableAction(module = "PERSON", action = "DELETE", description = "Exclusão de pessoa")
    @SecureOperation(module = "PERSON", value = "CONTENT_ADMIN")
    @Caching(evict = {
            @CacheEvict(value = "peoplePage", allEntries = true),
            @CacheEvict(value = "personById", key = "#id")
    })
    public void execute(Long id) {
        log.debug("Iniciando exclusão de pessoa. ID: {}", id);
        try {
            repo.deleteById(id);
            log.info("Pessoa excluída com sucesso. ID: {}", id);
        } catch (Exception e) {
            log.error("Erro ao excluir pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
