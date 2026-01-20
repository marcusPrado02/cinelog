package com.cine.cinelog.core.application.usecase.media;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.error.ErrorCode;
import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.policy.MediaPolicy;

@ExtendWith(MockitoExtension.class)
class UpdateMediaServiceTest {

    @Mock
    private MediaRepositoryPort repo;

    @Mock
    private MediaPolicy mediaPolicy;

    @InjectMocks
    private UpdateMediaService service;

    @Test
    void execute_whenMediaExists_updatesAndSaves() {
        Long id = 1L;
        Media current = mock(Media.class);
        Media data = mock(Media.class);
        Media updated = mock(Media.class);

        when(repo.findById(id)).thenReturn(Optional.of(current));
        when(current.updateFrom(data)).thenReturn(updated);
        when(repo.save(updated)).thenReturn(updated);

        Media result = service.execute(id, data);

        assertSame(updated, result);
        verify(mediaPolicy).validateInvariants(updated);
        verify(repo).save(updated);
    }

    @Test
    void execute_whenMediaNotFound_throwsDomainException() {
        Long id = 2L;
        Media data = mock(Media.class);

        when(repo.findById(id)).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () -> service.execute(id, data));
        assertTrue(ex.getMessage().contains("Media not found: " + id));
    }

    @Test
    void execute_whenValidationFails_propagatesAndDoesNotSave() {
        Long id = 3L;
        Media current = mock(Media.class);
        Media data = mock(Media.class);
        Media updated = mock(Media.class);

        when(repo.findById(id)).thenReturn(Optional.of(current));
        when(current.updateFrom(data)).thenReturn(updated);

        DomainException validationEx = DomainException.of(ErrorCode.MEDIA_NOT_FOUND, "validation failed");
        doThrow(validationEx).when(mediaPolicy).validateInvariants(updated);

        DomainException thrown = assertThrows(DomainException.class, () -> service.execute(id, data));
        assertSame(validationEx, thrown);
        verify(repo, never()).save(any());
    }
}