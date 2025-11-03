package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;

public interface UpdatePersonUseCase {
    Person execute(Long id, Person person);
}