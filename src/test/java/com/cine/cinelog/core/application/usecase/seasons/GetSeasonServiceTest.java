package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetSeasonServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    private GetSeasonService service;

    @BeforeEach
    void setUp() {
        service = new GetSeasonService(repo);
    }

    @Test
    void shouldReturnSeasonWhenFound() {
        Long id = 1L;
        Season season = mock(Season.class);
        when(repo.findById(id)).thenReturn(Optional.of(season));

        Season result = service.execute(id);

        assertSame(season, result);
        verify(repo).findById(id);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenNotFound() {
        Long id = 2L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("Season not found: " + id, ex.getMessage());
        verify(repo).findById(id);
    }
}