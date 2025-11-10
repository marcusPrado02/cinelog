package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPeopleServiceTest {

    @Mock
    private PersonRepositoryPort repo;

    @Test
    void execute_returnsListFromRepository_whenRepositoryHasPeople() {
        List<Person> people = Arrays.asList(mock(Person.class), mock(Person.class));
        when(repo.findAll()).thenReturn(people);

        ListPeopleService service = new ListPeopleService(repo);
        List<Person> result = service.execute();

        assertSame(people, result);
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_returnsEmptyList_whenRepositoryReturnsEmpty() {
        List<Person> empty = Collections.emptyList();
        when(repo.findAll()).thenReturn(empty);

        ListPeopleService service = new ListPeopleService(repo);
        List<Person> result = service.execute();

        assertTrue(result.isEmpty());
        assertSame(empty, result);
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }
}