package com.cine.cinelog.core.application.usecase;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.usecase.media.CreateMediaService;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class CreateMediaServiceTest {

    @Test
    void should_persist_media_when_valid() {
        var repo = mock(MediaRepositoryPort.class);
        var usecase = new CreateMediaService(repo);

        var input = new Media(null, "The Matrix", MediaType.MOVIE, 1999,
                null, null, null, null, null);

        when(repo.save(any())).then(invocation -> {
            Media m = invocation.getArgument(0);
            return m.withId(10L);
        });

        var saved = usecase.execute(input);

        var captor = ArgumentCaptor.forClass(Media.class);
        verify(repo).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("The Matrix");
        assertThat(saved.getId()).isEqualTo(10L);
    }

    @Test
    void should_throw_when_missing_title() {
        var repo = mock(MediaRepositoryPort.class);
        var usecase = new CreateMediaService(repo);

        // título inválido -> construtor já valida invariantes
        try {
            new Media(null, "   ", MediaType.MOVIE, 1999, null, null, null, null, null);
        } catch (IllegalArgumentException e) {
            assertThat(e).hasMessageContaining("title is required");
            return;
        }
        throw new AssertionError("Expected IllegalArgumentException");
    }
}
