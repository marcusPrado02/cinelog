package com.cine.cinelog.core.application.usecase.user;

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
class CreateUserServiceTest {

    @Mock
    private UserRepositoryPort userRepo;

    @InjectMocks
    private CreateUserService service;

    @Test
    void shouldSaveUserAndReturnSavedUser() {
        User input = mock(User.class);
        User saved = mock(User.class);

        when(userRepo.save(input)).thenReturn(saved);

        User result = service.execute(input);

        assertSame(saved, result);
        verify(userRepo).save(input);
    }

    @Test
    void shouldReturnNullWhenRepositoryReturnsNull() {
        when(userRepo.save(null)).thenReturn(null);

        User result = service.execute(null);

        assertNull(result);
        verify(userRepo).save(null);
    }

    @Test
    void shouldPropagateExceptionFromRepository() {
        User input = mock(User.class);
        RuntimeException ex = new RuntimeException("repo failure");
        when(userRepo.save(input)).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.execute(input));
        assertEquals("repo failure", thrown.getMessage());
        verify(userRepo).save(input);
    }
}