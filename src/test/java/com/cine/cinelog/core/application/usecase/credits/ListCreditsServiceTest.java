package com.cine.cinelog.core.application.usecase.credits;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.CreditRepositoryPort;
import com.cine.cinelog.core.domain.model.Credit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListCreditsServiceTest {

    @Mock
    private CreditRepositoryPort repo;

    @InjectMocks
    private ListCreditsService service;

    @Test
    void execute_shouldReturnRepositoryResult() {
        PageQuery pageQuery = mock(PageQuery.class);
        @SuppressWarnings("unchecked")
        PageResult<Credit> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Credit> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo, times(1)).findAll(pageQuery);
    }

    @Test
    void execute_shouldPassSamePageQueryInstanceToRepository() {
        PageQuery pageQuery = mock(PageQuery.class);
        @SuppressWarnings("unchecked")
        PageResult<Credit> pageResult = mock(PageResult.class);

        when(repo.findAll(any())).thenReturn(pageResult);

        service.execute(pageQuery);

        ArgumentCaptor<PageQuery> captor = ArgumentCaptor.forClass(PageQuery.class);
        verify(repo).findAll(captor.capture());
        assertSame(pageQuery, captor.getValue());
    }

    @Test
    void execute_withNullPageQuery_shouldForwardNullToRepository() {
        PageQuery pageQuery = new PageQuery(0, 10);
        @SuppressWarnings("unchecked")
        PageResult<Credit> expected = mock(PageResult.class);
        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Credit> actual = service.execute(pageQuery);

        assertSame(expected, actual);
        verify(repo).findAll(pageQuery);
    }
}
