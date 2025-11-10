package com.cine.cinelog.core.domain.policy.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.WatchEntry;

public class DefaultRatingPolicyTest {

    private DefaultRatingPolicy policy;

    @BeforeEach
    void setup() {
        // min = 1, max = 5, maxDaysSkew = 2
        policy = new DefaultRatingPolicy(1, 5, 2);
    }

    @Test
    void shouldThrowWhenRatingIsNull() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        assertThrows(DomainException.class, () -> policy.validateCanRate(entry, null, Instant.now()));
    }

    @Test
    void shouldThrowWhenRatingBelowMin() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        assertThrows(DomainException.class, () -> policy.validateCanRate(entry, 0, Instant.now()));
    }

    @Test
    void shouldThrowWhenRatingAboveMax() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        assertThrows(DomainException.class, () -> policy.validateCanRate(entry, 6, Instant.now()));
    }

    @Test
    void shouldThrowWhenNotWatched() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(null);

        assertThrows(DomainException.class, () -> policy.validateCanRate(entry, 3, Instant.now()));
    }

    @Test
    void shouldAllowWhenWhenIsNullAndWithinSkew() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        // when parameter null -> method should use Instant.now() internally and not
        // throw
        assertDoesNotThrow(() -> policy.validateCanRate(entry, 3, null));
    }

    @Test
    void shouldAllowWhenWithinMaxDaysSkew() {
        WatchEntry entry = mock(WatchEntry.class);
        LocalDate watched = LocalDate.now();
        when(entry.getWatchedAt()).thenReturn(watched);

        Instant whenInstant = Instant.now().plus(2, ChronoUnit.DAYS); // exactly at allowed skew
        assertDoesNotThrow(() -> policy.validateCanRate(entry, 4, whenInstant));
    }

    @Test
    void shouldThrowWhenBeyondMaxDaysSkew() {
        WatchEntry entry = mock(WatchEntry.class);
        LocalDate watched = LocalDate.now();
        when(entry.getWatchedAt()).thenReturn(watched);

        Instant whenInstant = Instant.now().plus(3, ChronoUnit.DAYS); // beyond allowed skew
        assertThrows(DomainException.class, () -> policy.validateCanRate(entry, 4, whenInstant));
    }
}