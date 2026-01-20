package com.cine.cinelog.core.application.usecase.seasons;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.SeasonRepositoryPort;
import com.cine.cinelog.core.domain.model.Season;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListSeasonsServiceTest {

    @Mock
    private SeasonRepositoryPort repo;

    @InjectMocks
    private ListSeasonsService service;

    @Test
    void execute_shouldDelegateToRepository_andReturnResult() {
        PageQuery pageQuery = mock(PageQuery.class);
        @SuppressWarnings("unchecked")
        PageResult<Season> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Season> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo).findAll(pageQuery);
    }

    @Test
    void execute_shouldReturnNull_whenRepositoryReturnsNull() {
        PageQuery pageQuery = new PageQuery(0, 10);
        PageResult<Season> emptyResult = new PageResult<>(Collections.emptyList(), 0, 10, 0, 0);

        when(repo.findAll(pageQuery)).thenReturn(emptyResult);

        PageResult<Season> actual = service.execute(pageQuery);

        assertSame(emptyResult, actual);
        verify(repo).findAll(pageQuery);
    }
}
