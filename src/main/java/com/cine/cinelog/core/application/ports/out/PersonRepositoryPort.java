package com.cine.cinelog.core.application.ports.out;

import com.cine.cinelog.core.domain.model.Person;
import java.util.List;
import java.util.Optional;

public interface PersonRepositoryPort {
    Person save(Person person);

    Optional<Person> findById(Long id);

    List<Person> findAll();

    void deleteById(Long id);
}
