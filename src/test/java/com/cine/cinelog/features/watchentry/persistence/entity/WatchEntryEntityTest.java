package com.cine.cinelog.features.watchentry.persistence.entity;

import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime updated = OffsetDateTime.now();
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

        OffsetDateTime before = OffsetDateTime.now();
        e.prePersist();
        OffsetDateTime created = e.getCreatedAt();
        OffsetDateTime updated = e.getUpdatedAt();

        assertNotNull(created);
        assertNotNull(updated);
        // created and updated should be at or after 'before'
        assertFalse(created.isBefore(before));
        assertFalse(updated.isBefore(before));
    }

    @Test
    void testPrePersistDoesNotOverwriteWhenPresent() {
        WatchEntryEntity e = new WatchEntryEntity();

        OffsetDateTime existingCreated = OffsetDateTime.now().minusDays(2);
        OffsetDateTime existingUpdated = OffsetDateTime.now().minusDays(1);
        e.setCreatedAt(existingCreated);
        e.setUpdatedAt(existingUpdated);

        e.prePersist();

        assertSame(existingCreated, e.getCreatedAt());
        assertSame(existingUpdated, e.getUpdatedAt());
    }

    @Test
    void testPreUpdateAlwaysUpdatesUpdatedAt() throws InterruptedException {
        WatchEntryEntity e = new WatchEntryEntity();

        OffsetDateTime old = OffsetDateTime.now().minusHours(1);
        e.setUpdatedAt(old);

        OffsetDateTime before = OffsetDateTime.now();
        e.preUpdate();
        OffsetDateTime updated = e.getUpdatedAt();

        assertNotNull(updated);
        assertFalse(updated.isBefore(before));
        assertTrue(updated.isAfter(old) || updated.equals(old) == false);
    }
}