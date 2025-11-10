package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUsersServiceTest {

    @Mock
    private UserRepositoryPort repo;

    @InjectMocks
    private ListUsersService service;

    @Test
    void execute_returnsEmptyList_whenRepositoryReturnsEmpty() {
        when(repo.findAll()).thenReturn(Collections.emptyList());

        List<User> result = service.execute();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_returnsUsers_whenRepositoryReturnsUsers() {
        User u1 = mock(User.class);
        User u2 = mock(User.class);
        List<User> users = Arrays.asList(u1, u2);
        when(repo.findAll()).thenReturn(users);

        List<User> result = service.execute();

        assertSame(users, result);
        assertEquals(2, result.size());
        verify(repo, times(1)).findAll();
        verifyNoMoreInteractions(repo);
    }
}