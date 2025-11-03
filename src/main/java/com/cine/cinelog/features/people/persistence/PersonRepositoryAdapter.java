package com.cine.cinelog.features.people.persistence;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.repository.PersonJpaRepository;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PersonRepositoryAdapter implements PersonRepositoryPort {

    private final PersonJpaRepository jpa;
    private final PersonMapper personMapper;

    public PersonRepositoryAdapter(PersonJpaRepository jpa, PersonMapper personMapper) {
        this.jpa = jpa;
        this.personMapper = personMapper;
    }

    @Override
    public Person save(Person person) {
        var e = personMapper.toEntity(person);
        var s = jpa.save(e);
        return personMapper.toDomain(s);
    }

    @Override
    public Optional<Person> findById(Long id) {
        return jpa.findById(id).map(personMapper::toDomain);
    }

    @Override
    public List<Person> findAll() {
        return jpa.findAll().stream().map(personMapper::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}