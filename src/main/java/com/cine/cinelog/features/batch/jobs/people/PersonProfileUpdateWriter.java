package com.cine.cinelog.features.batch.jobs.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Persiste pessoas com perfil enriquecido via TMDB.
 */
@Slf4j
@Component
public class PersonProfileUpdateWriter implements ItemWriter<Person> {

    private final PersonRepositoryPort personRepository;

    public PersonProfileUpdateWriter(PersonRepositoryPort personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public void write(Chunk<? extends Person> chunk) {
        for (Person person : chunk) {
            try {
                personRepository.save(person);
                log.debug("Perfil atualizado para pessoa id={}", person.getId());
            } catch (Exception e) {
                log.warn("Erro ao salvar perfil da pessoa id={}: {}", person.getId(), e.getMessage());
            }
        }
    }
}
