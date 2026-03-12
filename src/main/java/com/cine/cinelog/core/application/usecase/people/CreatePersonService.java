package com.cine.cinelog.core.application.usecase.people;

import java.util.Map;

import com.cine.cinelog.core.application.ports.in.person.CreatePersonUseCase;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.shared.observability.aop.AuditableAction;
import com.cine.cinelog.shared.observability.aop.Measured;

import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável por criar novos registros de pessoas (profissionais do
 * cinema e TV).
 *
 * <p>
 * Uma pessoa representa um profissional da indústria cinematográfica ou
 * televisiva
 * (atores, diretores, roteiristas, produtores, etc.) que pode ser associado a
 * mídias
 * através de créditos.
 *
 * @since 1.0
 * @see CreatePersonUseCase
 * @see PersonRepositoryPort
 * @see Person
 */
@Transactional
public class CreatePersonService implements CreatePersonUseCase {
    private static final Logger log = LoggerFactory.getLogger(CreatePersonService.class);

    private final PersonRepositoryPort repo;

    public CreatePersonService(PersonRepositoryPort repo) {
        this.repo = repo;
    }

    /**
     * Executa a criação de uma nova pessoa no sistema.
     *
     * @param person a pessoa a ser criada, com nome, data de nascimento e local de
     *               nascimento
     * @return a pessoa criada e persistida, com ID gerado
     */
    @Override
    @Observed(name = "person.create", contextualName = "create-person-service")
    @Measured("cinelog.service.person.create")
    @AuditableAction(module = "PERSON", action = "CREATE", description = "Criação de pessoa")
    @CacheEvict(value = "peoplePage", allEntries = true)
    public Person execute(Person person) {
        log.debug("Iniciando criação de pessoa. Nome: {}", person.getName());
        try {
            Person saved = repo.save(person);
            log.info("Pessoa criada com sucesso. ID: {}, Nome: {}", saved.getId(), saved.getName());
            return saved;
        } catch (Exception e) {
            log.error("Erro ao criar pessoa. Nome: {}, Erro: {}", person.getName(), e.getMessage(), e);
            throw e;
        }
    }
}
