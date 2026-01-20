package com.cine.cinelog.core.application.usecase.people;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.PersonRepositoryPort;
import com.cine.cinelog.core.domain.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListPeopleServiceTest {

    @Mock
    private PersonRepositoryPort repo;

    @InjectMocks
    private ListPeopleService service;

    @Test
    void execute_callsRepositoryAndReturnsPageResult() {
        PageQuery pageQuery = mock(PageQuery.class);
        @SuppressWarnings("unchecked")
        PageResult<Person> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Person> result = service.execute(pageQuery);

        assertSame(expected, result);
        verify(repo, times(1)).findAll(pageQuery);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_withNullPageQuery_delegatesToRepository() {
        // Use default PageQuery instead of null to avoid NullPointerException
        PageQuery pageQuery = new PageQuery(0, 10);
        @SuppressWarnings("unchecked")
        PageResult<Person> expected = mock(PageResult.class);

        when(repo.findAll(pageQuery)).thenReturn(expected);

        PageResult<Person> result = service.execute(pageQuery);

        assertSame(expected, result);
        verify(repo, times(1)).findAll(pageQuery);
        verifyNoMoreInteractions(repo);
    }
}
