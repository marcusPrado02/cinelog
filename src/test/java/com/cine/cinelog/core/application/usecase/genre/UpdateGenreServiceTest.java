package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateGenreServiceTest {

    @Mock
    private GenreRepositoryPort repo;

    @Test
    void execute_whenGenreExists_updatesNameAndSaves() {
        Long id = 1L;
        Genre existing = mock(Genre.class);
        Genre input = mock(Genre.class);

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(input.getName()).thenReturn("New Name");
        when(repo.save(existing)).thenReturn(existing);

        UpdateGenreService service = new UpdateGenreService(repo);
        Genre result = service.execute(id, input);

        verify(existing).setName("New Name");
        verify(repo).save(existing);
        assertSame(existing, result);
    }

    @Test
    void execute_whenGenreNotFound_throwsIllegalArgumentException() {
        Long id = 42L;
        Genre input = mock(Genre.class);

        when(repo.findById(id)).thenReturn(Optional.empty());

        UpdateGenreService service = new UpdateGenreService(repo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id, input));
        assertTrue(ex.getMessage().contains("Genre not found: " + id));
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }
}