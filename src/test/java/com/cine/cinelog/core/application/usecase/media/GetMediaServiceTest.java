package com.cine.cinelog.core.application.usecase.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.domain.model.Media;

class GetMediaServiceTest {

    private MediaRepositoryPort repo;
    private GetMediaService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(MediaRepositoryPort.class);
        service = new GetMediaService(repo);
    }

    @Test
    void shouldReturnMediaWhenFound() {
        Long id = 1L;
        Media media = Mockito.mock(Media.class);

        Mockito.when(repo.findById(id)).thenReturn(Optional.of(media));

        Media result = service.execute(id);

        assertSame(media, result);
        Mockito.verify(repo).findById(id);
    }

    @Test
    void shouldThrowWhenNotFound() {
        Long id = 2L;

        Mockito.when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("Mídia não encontrada: " + id, ex.getMessage());
        Mockito.verify(repo).findById(id);
    }
}