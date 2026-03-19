package com.cine.cinelog.core.application.usecase.watchentry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.cine.cinelog.core.application.pagination.PageResult;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDate;

@ExtendWith(MockitoExtension.class)
class ListWatchEntriesServiceTest {

    @Mock
    private WatchEntryRepositoryPort repo;

    @Test
    void execute_shouldThrowWhenUserIdIsNull() {
        ListWatchEntriesService service = new ListWatchEntriesService(repo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.execute(null, null, null, null, null, null, PageRequest.of(0, 10)));
        assertTrue(ex.getMessage().contains("userId is required"));
        verifyNoInteractions(repo);
    }

    @Test
    void execute_shouldDelegateToRepositoryAndReturnResult() {
        ListWatchEntriesService service = new ListWatchEntriesService(repo);

        Long userId = 1L;
        Long mediaId = 2L;
        Long episodeId = 3L;
        Integer minRating = 4;
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);
        PageRequest pageable = PageRequest.of(0, 10);

        @SuppressWarnings("unchecked")
        PageResult<WatchEntry> expected = mock(PageResult.class);

        when(repo.listByUser(userId, mediaId, episodeId, minRating, from, to, pageable)).thenReturn(expected);

        PageResult<WatchEntry> actual = service.execute(userId, mediaId, episodeId, minRating, from, to, pageable);

        assertSame(expected, actual);
        verify(repo, times(1)).listByUser(userId, mediaId, episodeId, minRating, from, to, pageable);
        verifyNoMoreInteractions(repo);
    }
}
