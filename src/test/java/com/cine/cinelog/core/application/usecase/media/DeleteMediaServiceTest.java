package com.cine.cinelog.core.application.usecase.media;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaDeletionPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteMediaServiceTest {

    @Test
    void execute_shouldCallRepositoryDeleteById() {
        MediaRepositoryPort repo = mock(MediaRepositoryPort.class);
        MediaDeletionPolicy deletionPolicy = mock(MediaDeletionPolicy.class);
        DeleteMediaService service = new DeleteMediaService(repo, deletionPolicy);

        Long id = 1L;
        when(repo.findById(id)).thenReturn(Optional.of(mock(Media.class)));

        service.execute(id);

        verify(repo).findById(id);
        verify(repo).deleteById(id);
    }

    @Test
    void execute_shouldPropagateExceptionFromRepository() {
        MediaRepositoryPort repo = mock(MediaRepositoryPort.class);
        MediaDeletionPolicy deletionPolicy = mock(MediaDeletionPolicy.class);
        DeleteMediaService service = new DeleteMediaService(repo, deletionPolicy);

        Long id = 2L;
        when(repo.findById(id)).thenReturn(Optional.of(mock(Media.class)));
        doThrow(new RuntimeException("delete failed")).when(repo).deleteById(id);

        assertThrows(RuntimeException.class, () -> service.execute(id));
        verify(repo).findById(id);
        verify(repo).deleteById(id);
    }
}
