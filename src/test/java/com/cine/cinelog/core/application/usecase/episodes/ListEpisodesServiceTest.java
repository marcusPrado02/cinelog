package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListEpisodesServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    @InjectMocks
    private ListEpisodesService service;

    @Test
    void execute_shouldReturnAllEpisodesFromRepository() {
        Episode e1 = mock(Episode.class);
        Episode e2 = mock(Episode.class);
        List<Episode> expected = Arrays.asList(e1, e2);
        when(repo.findAll()).thenReturn(expected);

        List<Episode> result = service.execute();

        assertEquals(expected, result);
        verify(repo, times(1)).findAll();
    }

    @Test
    void execute_shouldReturnEmptyListWhenRepositoryIsEmpty() {
        when(repo.findAll()).thenReturn(Collections.emptyList());

        List<Episode> result = service.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repo, times(1)).findAll();
    }
}