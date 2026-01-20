package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepositoryPort repo;

    @Mock
    private PageQuery pageQuery;

    @Mock
    private PageResult<User> pageResult;

    @InjectMocks
    private ListUsersService service;

    @Test
    void execute_shouldReturnPageResultFromRepository() {
        when(repo.findAll(pageQuery)).thenReturn(pageResult);

        PageResult<User> result = service.execute(pageQuery);

        assertSame(pageResult, result);
        verify(repo, times(1)).findAll(pageQuery);
    }

    @Test
    void execute_shouldPropagateExceptionFromRepository() {
        RuntimeException ex = new RuntimeException("repo failure");
        when(repo.findAll(pageQuery)).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.execute(pageQuery));
        assertSame(ex, thrown);
        verify(repo, times(1)).findAll(pageQuery);
    }
}