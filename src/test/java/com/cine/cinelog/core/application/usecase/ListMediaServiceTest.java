package com.cine.cinelog.core.application.usecase;

import org.junit.jupiter.api.Test;

import com.cine.cinelog.core.application.ports.out.MediaRepositoryPort;
import com.cine.cinelog.core.application.usecase.media.ListMediaService;
import com.cine.cinelog.core.domain.enums.MediaType;
import com.cine.cinelog.core.domain.model.Media;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ListMediaServiceTest {

    @Test
    void should_list_from_repo() {
        var repo = mock(MediaRepositoryPort.class);
        var usecase = new ListMediaService(repo);

        var item = new Media(1L, "Matrix", MediaType.MOVIE, 1999, null, null, null, null, null);
        when(repo.find(MediaType.MOVIE, "mat", 0, 10)).thenReturn(List.of(item));

        var result = usecase.execute(MediaType.MOVIE, "mat", 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Matrix");
        verify(repo).find(MediaType.MOVIE, "mat", 0, 10);
    }
}
