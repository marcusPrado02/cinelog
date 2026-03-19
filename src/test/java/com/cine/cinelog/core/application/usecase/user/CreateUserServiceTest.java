package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserPolicy;
import com.cine.cinelog.core.domain.policy.UserEmailUniquenessPolicy;
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

    @Mock
    private UserPolicy userPolicy;

    @Mock
    private UserEmailUniquenessPolicy uniqueness;

    @InjectMocks
    private CreateUserService service;

    @Test
    void shouldSaveUserAndReturnSavedUser() {
        User input = mock(User.class);
        User saved = mock(User.class);

        when(input.getName()).thenReturn("Test User");
        when(input.getEmail()).thenReturn("test@example.com");
        when(saved.getEmail()).thenReturn("test@example.com");

        when(userRepo.save(input)).thenReturn(saved);

        User result = service.execute(input);

        assertSame(saved, result);
        verify(userRepo).save(input);
    }

    @Test
    void shouldThrowNullPointerWhenRepositoryReturnsNull() {
        User input = mock(User.class);
        when(input.getName()).thenReturn("Test User");
        when(input.getEmail()).thenReturn("test@example.com");
        when(userRepo.save(input)).thenReturn(null);

        // Service will throw NullPointerException when trying to access saved.getId()
        assertThrows(NullPointerException.class, () -> service.execute(input));
        verify(userRepo).save(input);
    }

    @Test
    void shouldPropagateExceptionFromRepository() {
        User input = mock(User.class);
        when(input.getName()).thenReturn("Test User");
        when(input.getEmail()).thenReturn("test@example.com");

        RuntimeException ex = new RuntimeException("repo failure");
        when(userRepo.save(input)).thenThrow(ex);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> service.execute(input));
        assertNotNull(thrown);
        assertEquals("repo failure", thrown.getMessage());
        verify(userRepo).save(input);
    }
}
