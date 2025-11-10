package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteSeasonServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @InjectMocks
    private DeleteSeasonService service;

    @Test
    void execute_shouldCallRepositoryDeleteById_withGivenId() {
        Long id = 42L;

        service.execute(id);

        verify(repo, times(1)).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_withNull_shouldCallRepositoryDeleteById_withNull() {
        service.execute(null);

        verify(repo, times(1)).deleteById(null);
        verifyNoMoreInteractions(repo);
    }
}