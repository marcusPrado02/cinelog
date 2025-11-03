package com.cine.cinelog.core.application.usecase;

import org.junit.jupiter.api.Test;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.usecase.media.UpdateMediaService;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class UpdateMediaServiceTest {

    @Test
    void should_update_existing_media() {
        var repo = mock(MediaRepositoryPort.class);
        var usecase = new UpdateMediaService(repo);

        var existing = new Media(1L, "Old", MediaType.MOVIE, 2000, null, null, null, null, null);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var patch = new Media(null, "New", MediaType.MOVIE, 2000, null, null, null, null, null);

        var updated = usecase.execute(1L, patch);

        assertThat(updated.getId()).isEqualTo(1L);
        assertThat(updated.getTitle()).isEqualTo("New");
    }

    @Test
    void should_fail_when_not_found() {
        var repo = mock(MediaRepositoryPort.class);
        var usecase = new UpdateMediaService(repo);

        when(repo.findById(99L)).thenReturn(Optional.empty());

        var patch = new Media(null, "X", MediaType.MOVIE, 2000, null, null, null, null, null);

        assertThatThrownBy(() -> usecase.execute(99L, patch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Media not found");
    }
}
