package com.cine.cinelog.features.people.persistence;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.pagination.PageResultMapper;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import com.cine.cinelog.features.genres.persistence.entity.GenreEntity;
import com.cine.cinelog.features.people.mapper.PersonMapper;
import com.cine.cinelog.features.people.persistence.entity.PersonEntity;
import com.cine.cinelog.features.people.repository.PersonJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adaptador de repositório para persistência de Person.
 * Implementa a interface de porta de saída convertendo operações de domínio
 * em operações de persistência JPA.
 *
 * <p>
 * Este adaptador faz a ponte entre a camada de domínio e a infraestrutura,
 * realizando conversões entre Person e PersonEntity.
 * </p>
 *
 * @since 1.0
 * @see PersonRepositoryPort
 * @see PersonEntity
 * @see Person
 */
@Repository
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
    public PageResult<Person> findAll(PageQuery query) {
        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.fromString(query.direction()), query.sort()));

        Page<PersonEntity> page = jpa.findAll(pageable);

        return PageResultMapper.from(page, personMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public Optional<Person> findByTmdbPersonId(Long tmdbPersonId) {
        return jpa.findByTmdbPersonId(tmdbPersonId).map(personMapper::toDomain);
    }

    @Override
    public Optional<Person> findByName(String name) {
        return jpa.findByNameIgnoreCase(name).map(personMapper::toDomain);
    }
}
