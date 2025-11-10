package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class DeleteWatchEntryServiceTest {

    private WatchEntryRepositoryPort repo;
    private DeleteWatchEntryService service;

    @BeforeEach
    void setUp() {
        repo = mock(WatchEntryRepositoryPort.class);
        service = new DeleteWatchEntryService(repo);
    }

    @Test
    void execute_shouldCallRepoDeleteById_withGivenId() {
        Long id = 123L;
        service.execute(id);
        verify(repo, times(1)).deleteById(id);
    }

    @Test
    void execute_shouldPropagateExceptionFromRepository() {
        doThrow(new RuntimeException("repository error")).when(repo).deleteById(any());
        assertThrows(RuntimeException.class, () -> service.execute(1L));
    }
}