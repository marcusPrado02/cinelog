package com.cine.cinelog.core.application.usecase.people;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.person.UpdatePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por atualizar dados de pessoas existentes.
 * 
 * <p>
 * Permite alterar informações como nome, data de nascimento e local de
 * nascimento.
 * 
 * @since 1.0
 * @see UpdatePersonUseCase
 * @see PersonRepositoryPort
 */
@Transactional
public class UpdatePersonService implements UpdatePersonUseCase {
    private static final Logger log = LoggerFactory.getLogger(UpdatePersonService.class);

    private final PersonRepositoryPort repo;

    public UpdatePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a atualização de uma pessoa existente.
     * 
     * @param id     o identificador único da pessoa a ser atualizada
     * @param person os novos dados da pessoa
     * @return a pessoa atualizada e persistida
     * @throws DomainException com código {@link ErrorCode#GEN_NOT_FOUND} se a
     *                         pessoa não existir
     */
    @Override
    @Observed(name = "person.update", contextualName = "update-person-service")
    @Measured("cinelog.service.person.update")
    @AuditableAction(module = "PERSON", action = "UPDATE", description = "Atualização de pessoa")
    public Person execute(Long id, Person person) {
        log.debug("Iniciando atualização de pessoa. ID: {}", id);
        try {
            log.debug("Buscando pessoa existente. ID: {}", id);
            Person existing = repo.findById(id)
                    .orElseThrow(() -> DomainException.of(
                            ErrorCode.GEN_NOT_FOUND, "Person not found: " + id));

            log.debug("Pessoa encontrada. Aplicando atualizações");
            existing.setName(person.getName());
            existing.setBirthDate(person.getBirthDate());
            existing.setPlaceOfBirth(person.getPlaceOfBirth());

            Person saved = repo.save(existing);
            log.info("Pessoa atualizada com sucesso. ID: {}, Nome: {}", id, saved.getName());
            return saved;
        } catch (DomainException e) {
            log.warn("Pessoa não encontrada. ID: {}", id);
            throw e;
        } catch (Exception e) {
            log.error("Erro inesperado ao atualizar pessoa. ID: {}, Erro: {}", id, e.getMessage(), e);
            throw e;
        }
    }
}
