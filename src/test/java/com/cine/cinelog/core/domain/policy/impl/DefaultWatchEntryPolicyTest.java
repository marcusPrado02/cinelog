package com.cine.cinelog.core.domain.policy.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.WatchEntry;

class DefaultWatchEntryPolicyTest {

    private final DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy();

    @Test
    void validateCreate_nullEntry_throwsDomainException() {
        assertThrows(DomainException.class, () -> policy.validateCreate(null));
    }

    @Test
    void validateUpdate_nullEntry_throwsDomainException() {
        assertThrows(DomainException.class, () -> policy.validateUpdate(null));
    }

    @Test
    void validateCreate_trimsCommentAndSetsNullForBlank() {
        WatchEntry entry1 = mock(WatchEntry.class);
        when(entry1.getRating()).thenReturn(null);
        when(entry1.getComment()).thenReturn("  hello world  ");
        when(entry1.getWatchedAt()).thenReturn(LocalDate.now());
        policy.validateCreate(entry1);
        verify(entry1).setComment("hello world");

        WatchEntry entry2 = mock(WatchEntry.class);
        when(entry2.getRating()).thenReturn(null);
        when(entry2.getComment()).thenReturn("     ");
        when(entry2.getWatchedAt()).thenReturn(LocalDate.now());
        policy.validateCreate(entry2);
        verify(entry2).setComment(null);
    }

    @Test
    void validateCreate_commentTooLong_throwsDomainException() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getRating()).thenReturn(null);
        // build a string longer than 2000 chars
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2005; i++) {
            sb.append('a');
        }
        when(entry.getComment()).thenReturn(sb.toString());
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());
        assertThrows(DomainException.class, () -> policy.validateCreate(entry));
    }

    @Test
    void validateCreate_futureWatchedAt_throwsDomainException() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getRating()).thenReturn(null);
        when(entry.getComment()).thenReturn(null);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now().plusDays(1));
        assertThrows(DomainException.class, () -> policy.validateCreate(entry));
    }

    @Test
    void validateCreate_ratingWithoutWatchedAt_throwsDomainException() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getRating()).thenReturn(BigDecimal.valueOf(7));
        when(entry.getWatchedAt()).thenReturn(null);
        // Rating.of(7) should be valid, but absence of watchedAt must cause exception
        assertThrows(DomainException.class, () -> policy.validateCreate(entry));
    }

    @Test
    void validateCreate_validRating_setsNormalizedValue() {
        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getRating()).thenReturn(BigDecimal.valueOf(8));
        when(entry.getComment()).thenReturn(null);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now());
        // Should not throw and should call setRating with normalized value (expected 8)
        assertDoesNotThrow(() -> policy.validateCreate(entry));
        verify(entry).setRating(BigDecimal.valueOf(8));
    }
}