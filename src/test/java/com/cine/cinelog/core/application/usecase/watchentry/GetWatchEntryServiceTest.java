package com.cine.cinelog.core.application.usecase.watchentry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetWatchEntryServiceTest {

    @Mock
    private WatchEntryRepositoryPort repo;

    @Test
    void execute_shouldReturnWatchEntryWhenFound() {
        WatchEntry expected = org.mockito.Mockito.mock(WatchEntry.class);
        Long id = 1L;
        when(repo.findById(id)).thenReturn(Optional.of(expected));

        GetWatchEntryService service = new GetWatchEntryService(repo);
        WatchEntry actual = service.execute(id);

        assertSame(expected, actual);
        verify(repo).findById(id);
    }

    @Test
    void execute_shouldThrowIllegalArgumentExceptionWhenNotFound() {
        Long id = 42L;
        when(repo.findById(id)).thenReturn(Optional.empty());

        GetWatchEntryService service = new GetWatchEntryService(repo);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.execute(id));
        assertEquals("WatchEntry não encontrado: " + id, ex.getMessage());
        verify(repo).findById(id);
    }
}