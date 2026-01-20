package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetGenreServiceTest {

    @Mock
    private GenreRepositoryPort repo;

    @Test
    void execute_returnsGenreWhenFound() {
        Genre genre = org.mockito.Mockito.mock(Genre.class);
        when(repo.findById(1L)).thenReturn(Optional.of(genre));

        GetGenreService service = new GetGenreService(repo);
        Genre result = service.execute(1L);

        assertSame(genre, result);
    }

    @Test
    void execute_throwsWhenNotFound() {
        when(repo.findById(2L)).thenReturn(Optional.empty());

        GetGenreService service = new GetGenreService(repo);
        DomainException ex = assertThrows(DomainException.class, () -> service.execute(2L));

        assertTrue(ex.getMessage().contains("Genre not found: 2"));
    }
}