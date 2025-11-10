package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteEpisodeServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    private DeleteEpisodeService service;

    @BeforeEach
    void setUp() {
        service = new DeleteEpisodeService(repo);
    }

    @Test
    void shouldDeleteEpisodeById() {
        Long id = 1L;

        service.execute(id);

        verify(repo, times(1)).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryThrows() {
        Long id = 2L;
        doThrow(new RuntimeException("delete failed")).when(repo).deleteById(id);

        assertThrows(RuntimeException.class, () -> service.execute(id));

        verify(repo, times(1)).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldCallRepositoryWithNullIdWhenNullProvided() {
        service.execute(null);

        verify(repo, times(1)).deleteById(null);
        verifyNoMoreInteractions(repo);
    }
}