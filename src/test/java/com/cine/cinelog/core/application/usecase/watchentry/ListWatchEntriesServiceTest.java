package com.cine.cinelog.core.application.usecase.watchentry;

import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListWatchEntriesServiceTest {

    @Test
    void execute_whenUserIdIsNull_shouldThrowIllegalArgumentException() {
        WatchEntryRepositoryPort repo = mock(WatchEntryRepositoryPort.class);
        ListWatchEntriesService service = new ListWatchEntriesService(repo);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.execute(null, 1L, 1L, 5, LocalDate.now(), LocalDate.now(), Pageable.unpaged()));

        assertEquals("userId is required", ex.getMessage());
        verifyNoInteractions(repo);
    }

    @Test
    void execute_whenValidArguments_shouldDelegateToRepositoryAndReturnPage() {
        WatchEntryRepositoryPort repo = mock(WatchEntryRepositoryPort.class);
        ListWatchEntriesService service = new ListWatchEntriesService(repo);

        Long userId = 10L;
        Long mediaId = 20L;
        Long episodeId = 30L;
        Integer minRating = 7;
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to = LocalDate.of(2020, 12, 31);
        Pageable pageable = Pageable.unpaged();

        Page<WatchEntry> expectedPage = mock(Page.class);
        when(repo.listByUser(userId, mediaId, episodeId, minRating, from, to, pageable)).thenReturn(expectedPage);

        Page<WatchEntry> result = service.execute(userId, mediaId, episodeId, minRating, from, to, pageable);

        assertSame(expectedPage, result);
        verify(repo, times(1)).listByUser(userId, mediaId, episodeId, minRating, from, to, pageable);
        verifyNoMoreInteractions(repo);
    }
}