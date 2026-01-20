package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.events.DomainEventPublisherPort;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryUniquenessPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateWatchEntryServiceTest {

    @Mock
    private WatchEntryRepositoryPort repo;
    @Mock
    private WatchEntryPolicy policy;
    @Mock
    private WatchEntryUniquenessPolicy uniquenessPolicy;
    @Mock
    private DomainEventPublisherPort eventPublisher;

    @Test
    void execute_shouldDelegateToRepositoryAndReturnSavedEntry() {
        CreateWatchEntryService service = new CreateWatchEntryService(repo, policy, uniquenessPolicy, eventPublisher);

        WatchEntry input = mock(WatchEntry.class);
        WatchEntry saved = mock(WatchEntry.class);

        when(repo.save(input)).thenReturn(saved);

        WatchEntry result = service.execute(input);

        assertSame(saved, result, "service should return the object returned by repository.save");
        verify(repo, times(1)).save(input);
        verifyNoMoreInteractions(repo);
    }

    @Test
    void execute_withNull_shouldCallRepositoryAndReturnRepositoryResult() {
        CreateWatchEntryService service = new CreateWatchEntryService(repo, policy, uniquenessPolicy, eventPublisher);

        when(repo.save(null)).thenReturn(null);

        WatchEntry result = service.execute(null);

        assertNull(result, "service should return whatever repository.save returns for null");
        verify(repo, times(1)).save(null);
        verifyNoMoreInteractions(repo);
    }
}
