package com.cine.cinelog.core.application.usecase.watchentry;

import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.cine.cinelog.core.application.ports.out.WatchEntryRepositoryPort;
import com.cine.cinelog.core.domain.model.WatchEntry;
import com.cine.cinelog.core.domain.model.WatchEntryStatusType;
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
    private WatchEntry updated;

    @Mock
    private UpdateWatchEntryService service;

    @Mock
    private WatchEntryReferencePolicy referencePolicy;

    @BeforeEach
    void setUp() {
        service = new UpdateWatchEntryService(repo, watchPolicy, ratingPolicy, referencePolicy);
        // Always return entry when findById is called (entry.getId() = null by default
        // from mock)
        when(repo.findById(any())).thenReturn(Optional.of(entry));
        // updateFrom returns the updated mock
        when(entry.updateFrom(entry)).thenReturn(updated);
        // updated is not in PLANNING (statusType = COMPLETED by default behavior)
        lenient().when(updated.getStatusType()).thenReturn(WatchEntryStatusType.COMPLETED);
        // save returns updated
        when(repo.save(updated)).thenReturn(updated);
    }

    @Test
    void shouldValidateUpdate_andSave_whenNotRatingOperation() {
        when(updated.getRating()).thenReturn(null);
        when(updated.getComment()).thenReturn("no-rating");

        service.execute(entry.getId(), entry, false);

        verify(watchPolicy).validateUpdate(updated);
        verify(ratingPolicy, never()).validateCanRate(any(), any(), any());
        verify(updated).applyRating(null, "no-rating");
        verify(repo).save(updated);
    }

    @Test
    void shouldValidateCanRate_andSave_whenRatingOperation_andRatingPresent() {
        BigDecimal rating = BigDecimal.valueOf(4);
        when(updated.getRating()).thenReturn(rating);
        when(updated.getComment()).thenReturn("good");

        service.execute(entry.getId(), entry, true);

        verify(watchPolicy).validateUpdate(updated);
        verify(ratingPolicy).validateCanRate(eq(updated), eq(rating), any(Instant.class));
        verify(updated).applyRating(rating, "good");
        verify(repo).save(updated);
    }

    @Test
    void shouldNotCallValidateCanRate_whenRatingOperation_butNoRatingPresent() {
        when(updated.getRating()).thenReturn(null);
        when(updated.getComment()).thenReturn("empty");

        service.execute(entry.getId(), entry, true);

        verify(watchPolicy).validateUpdate(updated);
        verify(ratingPolicy, never()).validateCanRate(any(), any(), any());
        verify(updated).applyRating(null, "empty");
        verify(repo).save(updated);
    }
}
