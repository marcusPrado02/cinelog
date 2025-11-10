package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateSeasonServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @Mock
    private Season season;

    @Mock
    private Season savedSeason;

    @InjectMocks
    private CreateSeasonService service;

    @Test
    void shouldSaveAndReturnSeason() {
        when(repo.save(season)).thenReturn(savedSeason);

        Season result = service.execute(season);

        assertSame(savedSeason, result);
        verify(repo, times(1)).save(season);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateExceptionFromRepository() {
        when(repo.save(season)).thenThrow(new RuntimeException("db error"));

        assertThrows(RuntimeException.class, () -> service.execute(season));
        verify(repo, times(1)).save(season);
        verifyNoMoreInteractions(repo);
    }
}