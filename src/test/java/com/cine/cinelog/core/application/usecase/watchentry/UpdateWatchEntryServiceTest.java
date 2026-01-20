package com.cine.cinelog.core.application.usecase.watchentry;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.policy.RatingPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryPolicy;
import com.cine.cinelog.core.domain.policy.WatchEntryReferencePolicy;

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

    @Mock
    private UpdateWatchEntryService service;

    @Mock
    private WatchEntryReferencePolicy referencePolicy;

    @BeforeEach
    void setUp() {
        service = new UpdateWatchEntryService(repo, watchPolicy, ratingPolicy, referencePolicy);
    }

    @Test
    void shouldValidateUpdate_andSave_whenNotRatingOperation() {
        when(entry.getRating()).thenReturn(null);
        when(entry.getComment()).thenReturn("no-rating");
        when(repo.save(entry)).thenReturn(entry);

        service.execute(entry.getId(), entry, false);

        verify(watchPolicy).validateUpdate(entry);
        verify(ratingPolicy, never()).validateCanRate(any(), any(), any());
        verify(entry).applyRating(null, "no-rating");
        verify(repo).save(entry);
    }

    @Test
    void shouldValidateCanRate_andSave_whenRatingOperation_andRatingPresent() {
        BigDecimal rating = BigDecimal.valueOf(4);
        when(entry.getRating()).thenReturn(rating);
        when(entry.getComment()).thenReturn("good");
        when(repo.save(entry)).thenReturn(entry);

        service.execute(entry.getId(), entry, true);

        verify(watchPolicy).validateUpdate(entry);
        verify(ratingPolicy).validateCanRate(eq(entry), eq(rating), any(Instant.class));
        verify(entry).applyRating(rating, "good");
        verify(repo).save(entry);
    }

    @Test
    void shouldNotCallValidateCanRate_whenRatingOperation_butNoRatingPresent() {
        when(entry.getRating()).thenReturn(null);
        when(entry.getComment()).thenReturn("empty");
        when(repo.save(entry)).thenReturn(entry);

        service.execute(entry.getId(), entry, true);

        verify(watchPolicy).validateUpdate(entry);
        verify(ratingPolicy, never()).validateCanRate(any(), any(), any());
        verify(entry).applyRating(null, "empty");
        verify(repo).save(entry);
    }
}