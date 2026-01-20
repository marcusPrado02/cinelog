package com.cine.cinelog.core.application.usecase.user;

import com.cine.cinelog.core.application.usecase.user.DeleteUserService;
import com.cine.cinelog.core.application.ports.out.UserRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.User;
import com.cine.cinelog.core.domain.policy.UserDeletionPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeleteUserServiceTest {

    @Test
    public void execute_deletesWhenUserExists() {
        UserRepositoryPort repo = mock(UserRepositoryPort.class);
        UserDeletionPolicy policy = mock(UserDeletionPolicy.class);
        DeleteUserService service = new DeleteUserService(repo, policy);

        Long id = 1L;
        User user = mock(User.class);
        when(repo.findById(id)).thenReturn(Optional.of(user));

        service.execute(id);

        verify(policy, times(1)).validateDelete(user);
        verify(repo, times(1)).deleteById(id);
    }

    @Test
    public void execute_throwsWhenUserNotFound() {
        UserRepositoryPort repo = mock(UserRepositoryPort.class);
        UserDeletionPolicy policy = mock(UserDeletionPolicy.class);
        DeleteUserService service = new DeleteUserService(repo, policy);

        Long id = 2L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        assertThrows(DomainException.class, () -> service.execute(id));
        verify(policy, never()).validateDelete(any());
        verify(repo, never()).deleteById(anyLong());
    }

    @Test
    public void execute_propagatesPolicyExceptionAndDoesNotDelete() {
        UserRepositoryPort repo = mock(UserRepositoryPort.class);
        UserDeletionPolicy policy = mock(UserDeletionPolicy.class);
        DeleteUserService service = new DeleteUserService(repo, policy);

        Long id = 3L;
        User user = mock(User.class);
        when(repo.findById(id)).thenReturn(Optional.of(user));
        doThrow(DomainException.of(ErrorCode.USER_NOT_FOUND)).when(policy).validateDelete(user);

        assertThrows(DomainException.class, () -> service.execute(id));
        verify(repo, never()).deleteById(anyLong());
    }
}