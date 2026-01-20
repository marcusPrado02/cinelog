package com.cine.cinelog.core.application.usecase.genre;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.GenreRepositoryPort;
import com.cine.cinelog.core.domain.model.Genre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
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
    void executeShouldReturnPageResultFromRepository() {
        PageQuery pageQuery = mock(PageQuery.class);
        @SuppressWarnings("unchecked")
        PageResult<Genre> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Genre> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo).findAll(pageQuery);
    }

    @Test
    void executeWithNullPageQueryShouldCallRepositoryAndReturnNull() {
        PageQuery pageQuery = new PageQuery(0, 10);
        @SuppressWarnings("unchecked")
        PageResult<Genre> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Genre> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo).findAll(pageQuery);
    }
}
