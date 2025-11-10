package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetEpisodeServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    @Test
    void shouldReturnEpisodeWhenFound() {
        Long id = 1L;
        Episode episode = mock(Episode.class);
        when(repo.findById(id)).thenReturn(Optional.of(episode));

        GetEpisodeService service = new GetEpisodeService(repo);
        Episode result = service.execute(id);

        assertSame(episode, result);
        verify(repo, times(1)).findById(id);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNotFound() {
        Long id = 42L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        GetEpisodeService service = new GetEpisodeService(repo);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("Episode not found: " + id, ex.getMessage());
        verify(repo, times(1)).findById(id);
    }
}