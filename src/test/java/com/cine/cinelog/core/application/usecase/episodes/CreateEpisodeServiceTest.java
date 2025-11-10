package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateEpisodeServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    @InjectMocks
    private CreateEpisodeService service;

    @Test
    void shouldSaveEpisodeAndReturnSaved() {
        Episode toSave = mock(Episode.class);
        Episode saved = mock(Episode.class);

        when(repo.save(toSave)).thenReturn(saved);

        Episode result = service.execute(toSave);

        assertSame(saved, result);
        verify(repo, times(1)).save(toSave);
    }

    @Test
    void shouldPropagateExceptionFromRepository() {
        Episode toSave = mock(Episode.class);
        when(repo.save(toSave)).thenThrow(new RuntimeException("db error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.execute(toSave));
        assertEquals("db error", ex.getMessage());
        verify(repo, times(1)).save(toSave);
    }
}