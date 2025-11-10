package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateUserServiceTest {

    @Mock
    private UserRepositoryPort repo;

    @InjectMocks
    private UpdateUserService service;

    @Test
    void execute_updatesAndSavesExistingUser() {
        Long id = 1L;
        User existing = new User();
        existing.setName("Old Name");
        existing.setEmail("old@example.com");

        User incoming = new User();
        incoming.setName("New Name");
        incoming.setEmail("new@example.com");

        when(repo.findById(id)).thenReturn(Optional.of(existing));
        when(repo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = service.execute(id, incoming);

        assertSame(existing, result);
        assertEquals("New Name", existing.getName());
        assertEquals("new@example.com", existing.getEmail());
        verify(repo).findById(id);
        verify(repo).save(existing);
    }

    @Test
    void execute_whenUserNotFound_throwsIllegalArgumentException() {
        Long id = 42L;
        User incoming = new User();
        incoming.setName("Name");
        incoming.setEmail("email@example.com");

        when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id, incoming));
        assertTrue(ex.getMessage().contains("User not found: " + id));
        verify(repo).findById(id);
        verifyNoMoreInteractions(repo);
    }
}