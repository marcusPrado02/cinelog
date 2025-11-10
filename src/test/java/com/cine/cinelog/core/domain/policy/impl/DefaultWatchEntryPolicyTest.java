package com.cine.cinelog.core.domain.policy.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.WatchEntry;

class DefaultWatchEntryPolicyTest {

    @Test
    void shouldThrowWhenEntryIsNull() {
        DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy(10, true);

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateCreate(null));
        assertTrue(ex.getMessage().contains("watchEntry must not be null"));
    }

    @Test
    void shouldThrowWhenCommentTooLong() {
        DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy(5, false);

        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getComment()).thenReturn("this comment is definitely too long");
        when(entry.getWatchedAt()).thenReturn(null);

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateCreate(entry));
        assertTrue(ex.getMessage().contains("comment too long"));
    }

    @Test
    void shouldThrowWhenWatchedAtInFutureAndForbidden() {
        DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy(100, true);

        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getComment()).thenReturn(null);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now(ZoneOffset.UTC).plusDays(1));

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateCreate(entry));
        assertTrue(ex.getMessage().contains("watchedAt cannot be in the future"));
    }

    @Test
    void shouldAllowValidEntry() {
        DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy(10, true);

        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getComment()).thenReturn("ok");
        when(entry.getWatchedAt()).thenReturn(LocalDate.now(ZoneOffset.UTC)); // not after now

        assertDoesNotThrow(() -> policy.validateCreate(entry));
        assertDoesNotThrow(() -> policy.validateUpdate(entry));
    }

    @Test
    void shouldAllowFutureWhenNotForbidden() {
        DefaultWatchEntryPolicy policy = new DefaultWatchEntryPolicy(10, false);

        WatchEntry entry = mock(WatchEntry.class);
        when(entry.getComment()).thenReturn(null);
        when(entry.getWatchedAt()).thenReturn(LocalDate.now(ZoneOffset.UTC).plusDays(5));

        assertDoesNotThrow(() -> policy.validateCreate(entry));
    }
}