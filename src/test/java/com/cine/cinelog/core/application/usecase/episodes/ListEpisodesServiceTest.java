package com.cine.cinelog.core.application.usecase.episodes;

import com.cine.cinelog.core.application.pagination.PageQuery;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.EpisodeRepositoryPort;
import com.cine.cinelog.core.domain.model.Episode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ListEpisodesServiceTest {

    @Mock
    private EpisodeRepositoryPort repo;

    @InjectMocks
    private ListEpisodesService service;

    @Mock
    private PageQuery pageQuery;

    @SuppressWarnings("unchecked")
    @Mock
    private PageResult<Episode> pageResult;

    @Test
    void execute_shouldReturnPageResultFromRepository() {
        when(repo.findAll(pageQuery)).thenReturn(pageResult);

        PageResult<Episode> result = service.execute(pageQuery);

        assertSame(pageResult, result);
        verify(repo).findAll(pageQuery);
    }

    @Test
    void execute_shouldDelegateTheSamePageQueryInstanceToRepository() {
        when(repo.findAll(pageQuery)).thenReturn(pageResult);

        service.execute(pageQuery);

        // verify that the exact instance passed to service is forwarded to repository
        verify(repo).findAll(pageQuery);
    }
}