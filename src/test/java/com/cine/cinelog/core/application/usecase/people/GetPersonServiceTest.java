package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class GetPersonServiceTest {

    @Test
    void execute_returnsPerson_whenFound() {
        PersonRepositoryPort repo = Mockito.mock(PersonRepositoryPort.class);
        Person person = Mockito.mock(Person.class);
        Long id = 1L;

        Mockito.when(repo.findById(id)).thenReturn(Optional.of(person));

        GetPersonService service = new GetPersonService(repo);

        Person result = service.execute(id);

        assertSame(person, result);
        Mockito.verify(repo).findById(id);
    }

    @Test
    void execute_throwsIllegalArgumentException_whenNotFound() {
        PersonRepositoryPort repo = Mockito.mock(PersonRepositoryPort.class);
        Long id = 42L;

        Mockito.when(repo.findById(id)).thenReturn(Optional.empty());

        GetPersonService service = new GetPersonService(repo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("Person not found: " + id, ex.getMessage());
        Mockito.verify(repo).findById(id);
    }
}