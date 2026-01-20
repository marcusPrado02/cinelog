package com.cine.cinelog.core.application.usecase.media;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ListMediaServiceTest {

    @Mock
    private MediaRepositoryPort repo;

    @InjectMocks
    private ListMediaService service;

    @Test
    void execute_delegatesToRepository_andReturnsResult() {
        PageQuery pageQuery = mock(PageQuery.class);
        PageResult<Media> expected = mock(PageResult.class);

        when(repo.listAll(pageQuery)).thenReturn(expected);

        PageResult<Media> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo, times(1)).listAll(pageQuery);
    }

    @Test
    void execute_forwardsNullPageQueryToRepository() {
        PageQuery pageQuery = new PageQuery(0, 10);
        PageResult<Media> expected = mock(PageResult.class);

        when(repo.listAll(pageQuery)).thenReturn(expected);

        PageResult<Media> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo).listAll(pageQuery);
    }

    @Test
    void execute_passesExactPageQueryInstanceToRepository() {
        PageQuery pageQuery = mock(PageQuery.class);
        PageResult<Media> expected = mock(PageResult.class);

        when(repo.listAll(any())).thenReturn(expected);

        service.execute(pageQuery);

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(repo).listAll(captor.capture());
        assertSame(pageQuery, captor.getValue());
    }
}
