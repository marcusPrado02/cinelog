package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

class CreatePersonServiceTest {

    @Test
    void execute_shouldCallRepositorySave_andReturnSavedPerson() {
        // arrange
        PersonRepositoryPort repo = mock(PersonRepositoryPort.class);
        CreatePersonService service = new CreatePersonService(repo);

        Person input = mock(Person.class);
        Person saved = mock(Person.class);
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

        when(repo.save(null)).thenReturn(null);

        // act
        Person result = service.execute(null);

        // assert
        verify(repo, times(1)).save(null);
        assertSame(null, result);
    }
}