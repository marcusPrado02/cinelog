package com.cine.cinelog.core.application.usecase.media;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteMediaServiceTest {

    @Test
    void execute_shouldCallRepositoryDeleteById() {
        MediaRepositoryPort repo = mock(MediaRepositoryPort.class);
        DeleteMediaService service = new DeleteMediaService(repo);

        Long id = 1L;
        service.execute(id);

        verify(repo).deleteById(id);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_shouldPropagateExceptionFromRepository() {
        MediaRepositoryPort repo = mock(MediaRepositoryPort.class);
        DeleteMediaService service = new DeleteMediaService(repo);

        Long id = 2L;
        doThrow(new RuntimeException("delete failed")).when(repo).deleteById(id);

        assertThrows(RuntimeException.class, () -> service.execute(id));
        verify(repo).deleteById(id);
    }
}