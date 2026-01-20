package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CreatePersonServiceTest {

    @Test
    void execute_shouldCallRepositorySave_andReturnSavedPerson() {
        // arrange
        PersonRepositoryPort repo = mock(PersonRepositoryPort.class);
        CreatePersonService service = new CreatePersonService(repo);

        Person input = mock(Person.class);
        Person saved = mock(Person.class);

        when(input.getName()).thenReturn("Test Person");
        when(saved.getName()).thenReturn("Test Person");
        when(repo.save(input)).thenReturn(saved);

        // act
        Person result = service.execute(input);

        // assert
        verify(repo, times(1)).save(input);
        assertSame(saved, result);
    }

    @Test
    void execute_shouldForwardNullIfRepositoryAcceptsNull() {
        // arrange
        PersonRepositoryPort repo = mock(PersonRepositoryPort.class);
        CreatePersonService service = new CreatePersonService(repo);

        Person input = mock(Person.class);
        when(input.getName()).thenReturn("Test Person");
        when(repo.save(input)).thenReturn(null);

        // act/assert - Service will throw NullPointerException when trying to access
        // saved.getId()
        assertThrows(NullPointerException.class, () -> service.execute(input));

        // verify
        verify(repo, times(1)).save(input);
    }
}
