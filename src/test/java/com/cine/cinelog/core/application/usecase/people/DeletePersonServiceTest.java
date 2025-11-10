package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeletePersonServiceTest {

    @Mock
    private PersonRepositoryPort repo;

    @InjectMocks
    private DeletePersonService service;

    @Test
    void execute_shouldCallRepositoryDeleteById_withGivenId() {
        Long id = 42L;

        service.execute(id);

        verify(repo, times(1)).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_shouldPropagateException_whenRepositoryThrows() {
        Long id = 7L;
        RuntimeException ex = new RuntimeException("repository failure");
        doThrow(ex).when(repo).deleteById(id);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.execute(id));
        assertSame(ex, thrown);

        verify(repo, times(1)).deleteById(id);
    }
}