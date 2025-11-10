package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class DeleteGenreServiceTest {

    @Mock
    private GenreRepositoryPort repo;

    @InjectMocks
    private DeleteGenreService service;

    @Test
    void execute_shouldCallRepositoryDeleteById_withGivenId() {
        Long id = 1L;

        service.execute(id);

        verify(repo).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_shouldCallRepositoryDeleteById_withNullId() {
        Long id = null;

        service.execute(id);

        verify(repo).deleteById(id);
        verifyNoMoreInteractions(repo);
    }
}