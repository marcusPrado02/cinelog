package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateEpisodeServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    @InjectMocks
    private UpdateEpisodeService service;

    @Test
    void shouldUpdateEpisodeSuccessfully() {
        Long id = 1L;

        Episode existing = new Episode();
        existing.setName("Old Name");
        existing.setEpisodeNumber(1);
        existing.setSeasonId(10L);
        existing.setAirDate(LocalDate.of(2020, 1, 1));

        Episode update = new Episode();
        update.setName("New Name");
        update.setEpisodeNumber(2);
        update.setSeasonId(11L);
        update.setAirDate(LocalDate.of(2021, 2, 2));

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Episode result = service.execute(id, update);

        ArgumentCaptor<Episode> captor = ArgumentCaptor.forClass(Episode.class);
        verify(repo).save(captor.capture());

        Episode saved = captor.getValue();
        assertEquals("New Name", saved.getName());
        assertEquals(2, saved.getEpisodeNumber());
        assertEquals(11L, saved.getSeasonId());
        assertEquals(LocalDate.of(2021, 2, 2), saved.getAirDate());

        // returned object should reflect saved values
        assertEquals(saved, result);
    }

    @Test
    void shouldThrowWhenEpisodeNotFound() {
        Long id = 42L;
        Episode update = new Episode();
        update.setName("Doesn't matter");

        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id, update));
        assertTrue(ex.getMessage().contains("Episode not found"));
        verify(repo, never()).save(any());
    }
}