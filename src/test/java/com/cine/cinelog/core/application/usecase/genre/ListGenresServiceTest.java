package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListGenresServiceTest {

    @Mock
    private GenreRepositoryPort repo;

    private ListGenresService service;

    @BeforeEach
    void setUp() {
        service = new ListGenresService(repo);
    }

    @Test
    void shouldReturnGenresFromRepository() {
        List<Genre> expected = Arrays.asList(mock(Genre.class), mock(Genre.class));
        when(repo.findAll()).thenReturn(expected);

        List<Genre> result = service.execute();

        assertSame(expected, result, "Service should return the same list instance provided by the repository");
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void shouldReturnEmptyListWhenRepositoryReturnsEmpty() {
        List<Genre> expected = Collections.emptyList();
        when(repo.findAll()).thenReturn(expected);

        List<Genre> result = service.execute();

        assertSame(expected, result, "Service should return the empty list from repository");
        assertTrue(result.isEmpty());
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }
}