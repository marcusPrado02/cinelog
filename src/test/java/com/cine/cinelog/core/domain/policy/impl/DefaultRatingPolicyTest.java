package com.cine.cinelog.core.domain.policy.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.WatchEntry;

public class DefaultRatingPolicyTest {

    @Test
    void nullRatingShouldThrowInvalidArgument() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        DomainException ex = assertThrows(DomainException.class,
                () -> policy.validateCanRate(entry, null, Instant.now()));
        assertTrue(ex.getMessage().contains("rating must not be null"));
    }

    @Test
    void ratingBelowMinShouldThrowRatingNotAllowed() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        DomainException ex = assertThrows(DomainException.class,
                () -> policy.validateCanRate(entry, BigDecimal.valueOf(-1), Instant.now()));
        assertTrue(ex.getMessage().contains("rating out of bounds"));
    }

    @Test
    void ratingAboveMaxShouldThrowRatingNotAllowed() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());

        DomainException ex = assertThrows(DomainException.class,
                () -> policy.validateCanRate(entry, BigDecimal.valueOf(11), Instant.now()));
        assertTrue(ex.getMessage().contains("rating out of bounds"));
    }

    @Test
    void cannotRateWhenNotWatchedShouldThrowRatingNotAllowed() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getWatchedAt()).thenReturn(null);

        DomainException ex = assertThrows(DomainException.class,
                () -> policy.validateCanRate(entry, BigDecimal.valueOf(5), Instant.now()));
        assertTrue(ex.getMessage().contains("cannot rate before marking as watched"));
    }

    @Test
    void ratingWithinAllowedDaysSkewShouldNotThrow() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        LocalDate watched = LocalDate.of(2025, 1, 10);
        when(entry.getWatchedAt()).thenReturn(watched);

        Instant when = watched.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC);
        assertDoesNotThrow(() -> policy.validateCanRate(entry, BigDecimal.valueOf(5), when));
    }

    @Test
    void ratingBeyondAllowedDaysSkewShouldThrowRatingNotAllowed() {
        DefaultRatingPolicy policy = new DefaultRatingPolicy(0, 10, 2);
        WatchEntry entry = mock(WatchEntry.class);
        LocalDate watched = LocalDate.of(2025, 1, 10);
        when(entry.getWatchedAt()).thenReturn(watched);

        Instant when = watched.plusDays(3).atStartOfDay().toInstant(ZoneOffset.UTC);
        DomainException ex = assertThrows(DomainException.class,
                () -> policy.validateCanRate(entry, BigDecimal.valueOf(5), when));
        assertTrue(ex.getMessage().contains("rating time too far from watchedAt"));
    }
}