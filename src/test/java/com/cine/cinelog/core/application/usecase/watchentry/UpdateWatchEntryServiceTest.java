package com.cine.cinelog.core.application.usecase.watchentry;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;

@ExtendWith(MockitoExtension.class)
class UpdateWatchEntryServiceTest {

    @Mock
    private WatchEntryRepositoryPort repo;

    @Mock
    private WatchEntryPolicy watchPolicy;

    @Mock
    private RatingPolicy ratingPolicy;

    @Mock
    private WatchEntry entry;

    @InjectMocks
    private UpdateWatchEntryService service;

    @Test
    void shouldValidateRateApplyAndSave_whenRatingOperationAndRatingPresent() {
        Integer rating = 5;
        String comment = "good";
        WatchEntry saved = mock(WatchEntry.class);

        when(entry.getRating()).thenReturn(rating);
        when(entry.getComment()).thenReturn(comment);
        when(repo.save(entry)).thenReturn(saved);

        WatchEntry result = service.execute(entry, true);

        verify(watchPolicy).validateUpdate(entry);
        verify(ratingPolicy).validateCanRate(eq(entry), eq(rating), any(Instant.class));
        verify(entry).applyRating(rating, comment);
        verify(repo).save(entry);
        assertSame(saved, result);
    }

    @Test
    void shouldNotCallValidateCanRate_whenRatingOperationButRatingIsNull() {
        String comment = "no rating";

        when(entry.getRating()).thenReturn(null);
        when(entry.getComment()).thenReturn(comment);
        when(repo.save(entry)).thenReturn(entry);

        WatchEntry result = service.execute(entry, true);

        verify(watchPolicy).validateUpdate(entry);
        verifyNoInteractions(ratingPolicy);
        verify(entry).applyRating(null, comment);
        verify(repo).save(entry);
        assertSame(entry, result);
    }

    @Test
    void shouldNotCallValidateCanRate_whenNotRatingOperationEvenIfRatingPresent() {
        Integer rating = 4;
        String comment = "skip rating";

        when(entry.getRating()).thenReturn(rating);
        when(entry.getComment()).thenReturn(comment);
        when(repo.save(entry)).thenReturn(entry);

        WatchEntry result = service.execute(entry, false);

        verify(watchPolicy).validateUpdate(entry);
        verifyNoInteractions(ratingPolicy);
        verify(entry).applyRating(rating, comment);
        verify(repo).save(entry);
        assertSame(entry, result);
    }
}