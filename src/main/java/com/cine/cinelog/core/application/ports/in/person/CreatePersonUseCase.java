package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;

public interface CreatePersonUseCase {
    Person execute(Person person);
}