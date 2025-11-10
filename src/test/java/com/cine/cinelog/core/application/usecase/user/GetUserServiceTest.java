package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetUserServiceTest {

    @Mock
    private UserRepositoryPort repo;

    @Mock
    private User user;

    private GetUserService service;

    @BeforeEach
    void setUp() {
        service = new GetUserService(repo);
    }

    @Test
    void execute_whenUserExists_returnsUser() {
        Long id = 1L;
        when(repo.findById(id)).thenReturn(Optional.of(user));

        User result = service.execute(id);

        assertSame(user, result);
        verify(repo, times(1)).findById(id);
    }

    @Test
    void execute_whenUserNotFound_throwsIllegalArgumentException() {
        Long id = 2L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("User not found: " + id, ex.getMessage());
        verify(repo, times(1)).findById(id);
    }
}