package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateGenreServiceTest {

    @Mock
    private GenreRepositoryPort repo;

    private CreateGenreService service;

    @BeforeEach
    void setUp() {
        service = new CreateGenreService(repo);
    }

    @Test
    void shouldCallRepositorySaveAndReturnSavedGenre() {
        Genre input = mock(Genre.class);
        Genre saved = mock(Genre.class);

        when(repo.save(input)).thenReturn(saved);

        Genre result = service.execute(input);

        assertSame(saved, result);
        verify(repo, times(1)).save(input);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldPropagateExceptionFromRepository() {
        Genre input = mock(Genre.class);
        RuntimeException ex = new RuntimeException("db error");

        when(repo.save(input)).thenThrow(ex);

        assertThrows(RuntimeException.class, () -> service.execute(input));
        verify(repo, times(1)).save(input);
        verifyNoMoreInteractions(repo);
    }
}