package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteUserServiceTest {

    @Mock
    private UserRepositoryPort repo;

    @Test
    void shouldDeleteUserById() {
        DeleteUserService service = new DeleteUserService(repo);

        Long id = 42L;
        service.execute(id);

        verify(repo, times(1)).deleteById(id);
    }

    @Test
    void shouldPropagateExceptionWhenRepositoryThrows() {
        DeleteUserService service = new DeleteUserService(repo);

        Long id = 99L;
        doThrow(new RuntimeException("db error")).when(repo).deleteById(id);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.execute(id));
        assertEquals("db error", ex.getMessage());

        verify(repo, times(1)).deleteById(id);
    }
}