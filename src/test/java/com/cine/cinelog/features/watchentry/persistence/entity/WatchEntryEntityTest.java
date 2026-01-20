package com.cine.cinelog.features.watchentry.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class WatchEntryEntityTest {

    @Test
    void testGettersAndSetters() {
        WatchEntryEntity e = new WatchEntryEntity();

        e.setId(123L);
        e.setUserId(10L);
        e.setMediaId(20L);
        e.setEpisodeId(30L);
        e.setRating(5);
        e.setComment("Great!");
        e.setWatchedAt(LocalDate.of(2025, 1, 2));

        LocalDateTime created = LocalDateTime.now().minusDays(1);
        LocalDateTime updated = LocalDateTime.now();
        e.setCreatedAt(created);
        e.setUpdatedAt(updated);

        assertEquals(123L, e.getId());
        assertEquals(10L, e.getUserId());
        assertEquals(20L, e.getMediaId());
        assertEquals(30L, e.getEpisodeId());
        assertEquals(5, e.getRating());
        assertEquals("Great!", e.getComment());
        assertEquals(LocalDate.of(2025, 1, 2), e.getWatchedAt());
        assertSame(created, e.getCreatedAt());
        assertSame(updated, e.getUpdatedAt());
    }

    @Test
    void testPrePersistSetsTimestampsWhenNull() {
        WatchEntryEntity e = new WatchEntryEntity();

        assertNull(e.getCreatedAt());
        assertNull(e.getUpdatedAt());

        LocalDateTime before = LocalDateTime.now();
        e.prePersist();
        LocalDateTime created = e.getCreatedAt();
        LocalDateTime updated = e.getUpdatedAt();

        assertNotNull(created);
        assertNotNull(updated);
        // created and updated should be at or after 'before'
        assertFalse(created.isBefore(before));
        assertFalse(updated.isBefore(before));
    }

    @Test
    void testPrePersistDoesNotOverwriteWhenPresent() {
        WatchEntryEntity e = new WatchEntryEntity();

        LocalDateTime existingCreated = LocalDateTime.now().minusDays(2);
        LocalDateTime existingUpdated = LocalDateTime.now().minusDays(1);
        e.setCreatedAt(existingCreated);
        e.setUpdatedAt(existingUpdated);

        e.prePersist();

        assertSame(existingCreated, e.getCreatedAt());
        assertSame(existingUpdated, e.getUpdatedAt());
    }

    @Test
    void testPreUpdateAlwaysUpdatesUpdatedAt() throws InterruptedException {
        WatchEntryEntity e = new WatchEntryEntity();

        LocalDateTime old = LocalDateTime.now().minusHours(1);
        e.setUpdatedAt(old);

        LocalDateTime before = LocalDateTime.now();
        e.preUpdate();
        LocalDateTime updated = e.getUpdatedAt();

        assertNotNull(updated);
        assertFalse(updated.isBefore(before));
        assertTrue(updated.isAfter(old) || updated.equals(old) == false);
    }
}