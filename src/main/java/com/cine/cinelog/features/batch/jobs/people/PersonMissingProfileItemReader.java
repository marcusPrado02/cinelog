package com.cine.cinelog.features.batch.jobs.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * Leitor que retorna pessoas com tmdbPersonId definido mas sem profileUrl ou
 * biography para enriquecimento via TMDB.
 */
@Component
@StepScope
public class PersonMissingProfileItemReader implements ItemReader<Person> {

    private final PersonRepositoryPort personRepository;

    private Iterator<Person> iterator;

    public PersonMissingProfileItemReader(PersonRepositoryPort personRepository) {
        this.personRepository = personRepository;
    }

    @Override
    public Person read() {
        if (iterator == null) {
            List<Person> missing = personRepository.findAllMissingProfile();
            iterator = missing.iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }
}
