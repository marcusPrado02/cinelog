package com.cine.cinelog.core.application.ports.in.person;

import com.cine.cinelog.core.domain.model.Person;
import java.util.List;

public interface ListPeopleUseCase {
    List<Person> execute();
}