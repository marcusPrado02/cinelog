package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import com.cine.cinelog.core.domain.policy.SeasonDeletionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteSeasonServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @Mock
    private SeasonDeletionPolicy deletionPolicy;

    @InjectMocks
    private DeleteSeasonService service;

    @Test
    void execute_shouldCallRepositoryDeleteById_withGivenId() {
        Long id = 42L;
        when(repo.findById(id)).thenReturn(Optional.of(new Season()));

        service.execute(id);

        verify(repo).findById(id);
        verify(repo, times(1)).deleteById(id);
    }

    @Test
    void execute_withNull_shouldCallRepositoryDeleteById_withNull() {
        when(repo.findById(null)).thenReturn(Optional.of(new Season()));

        service.execute(null);

        verify(repo).findById(null);
        verify(repo, times(1)).deleteById(null);
    }
}
