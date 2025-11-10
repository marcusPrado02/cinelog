package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateSeasonServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @InjectMocks
    private UpdateSeasonService service;

    @Test
    void execute_whenSeasonExists_updatesFieldsAndSaves() {
        Long id = 1L;

        Season existing = new Season();
        existing.setSeasonNumber(1);
        existing.setName("Old Name");
        existing.setMediaId(10L);

        Season input = new Season();
        input.setSeasonNumber(2);
        input.setName("New Name");
        input.setMediaId(20L);

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Season result = service.execute(id, input);

        ArgumentCaptor<Season> captor = ArgumentCaptor.forClass(Season.class);
        verify(repo).save(captor.capture());
        Season saved = captor.getValue();

        assertEquals(input.getSeasonNumber(), saved.getSeasonNumber());
        assertEquals(input.getName(), saved.getName());
        assertEquals(input.getMediaId(), saved.getMediaId());

        // returned value should be the saved entity
        assertSame(saved, result);
    }

    @Test
    void execute_whenSeasonNotFound_throwsIllegalArgumentException() {
        Long id = 42L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.execute(id, new Season()));
        assertEquals("Season not found: " + id, ex.getMessage());

        verify(repo, never()).save(any());
    }
}