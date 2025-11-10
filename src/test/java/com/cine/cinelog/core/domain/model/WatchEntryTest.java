package com.cine.cinelog.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

public class WatchEntryTest {

    @Test
    void constructorThrowsIfUserIdIsNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new WatchEntry(null, null, 1L, null, 5, "c", LocalDate.now(),
                        OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC)));
        assertEquals("userId is required", ex.getMessage());
    }

    @Test
    void constructorThrowsIfNoMediaOrEpisodeProvided() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> new WatchEntry(null, 1L, null, null, 5, "c", LocalDate.now(),
                        OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC)));
        assertEquals("Either mediaId or episodeId must be provided", ex.getMessage());
    }

    @Test
    void constructorAllowsNullRatingButRejectsOutOfRange() {
        // null rating is allowed
        assertDoesNotThrow(() -> new WatchEntry(null, 1L, 2L, null, null, "c", LocalDate.now(),
                OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC)));

        IllegalArgumentException exLow = assertThrows(IllegalArgumentException.class,
                () -> new WatchEntry(null, 1L, 2L, null, -1, "c", LocalDate.now(),
                        OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC)));
        assertEquals("rating must be 0..10", exLow.getMessage());

        IllegalArgumentException exHigh = assertThrows(IllegalArgumentException.class,
                () -> new WatchEntry(null, 1L, 2L, null, 11, "c", LocalDate.now(),
                        OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC)));
        assertEquals("rating must be 0..10", exHigh.getMessage());
    }

    @Test
    void withIdReturnsNewInstanceWithProvidedIdAndKeepsOtherFields() {
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime updated = created;
        WatchEntry original = new WatchEntry(1L, 10L, 20L, null, 4, "orig", LocalDate.of(2020, 1, 1), created, updated);

        WatchEntry withNewId = original.withId(999L);

        assertNotSame(original, withNewId);
        assertEquals(999L, withNewId.getId());
        assertEquals(original.getUserId(), withNewId.getUserId());
        assertEquals(original.getMediaId(), withNewId.getMediaId());
        assertEquals(original.getEpisodeId(), withNewId.getEpisodeId());
        assertEquals(original.getRating(), withNewId.getRating());
        assertEquals(original.getComment(), withNewId.getComment());
        assertEquals(original.getWatchedAt(), withNewId.getWatchedAt());
        assertEquals(original.getCreatedAt(), withNewId.getCreatedAt());
        assertEquals(original.getUpdatedAt(), withNewId.getUpdatedAt());
    }

    @Test
    void equalsAndHashCodeUseIdOnly() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        WatchEntry a1 = new WatchEntry(1L, 1L, 2L, null, 3, "a", LocalDate.now(), now, now);
        WatchEntry a2 = new WatchEntry(1L, 99L, null, 5L, 9, "different", LocalDate.now(), now, now);
        WatchEntry b = new WatchEntry(2L, 1L, 2L, null, 3, "a", LocalDate.now(), now, now);

        assertEquals(a1, a2);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertNotEquals(a1, b);
        assertNotEquals(a1.hashCode(), b.hashCode());

        // two null ids are considered equal by the implementation
        WatchEntry n1 = new WatchEntry(null, 1L, 2L, null, 1, null, LocalDate.now(), now, now);
        WatchEntry n2 = new WatchEntry(null, 2L, 2L, null, 2, "x", LocalDate.now(), now, now);
        assertEquals(n1, n2);
    }

    @Test
    void applyRatingSetsRatingTrimsNonBlankCommentAndUpdatesUpdatedAt() throws InterruptedException {
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime oldUpdated = created;
        WatchEntry e = new WatchEntry(1L, 1L, 2L, null, 2, "old", LocalDate.now(), created, oldUpdated);

        e.applyRating(7, "  trimmed  ");

        assertEquals(7, e.getRating());
        assertEquals("trimmed", e.getComment());
        assertNotNull(e.getUpdatedAt());
        assertTrue(e.getUpdatedAt().isAfter(oldUpdated));
    }

    @Test
    void applyRatingWithBlankCommentDoesNotOverwriteExistingComment() {
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime oldUpdated = created;
        WatchEntry e = new WatchEntry(1L, 1L, 2L, null, 2, "keep", LocalDate.now(), created, oldUpdated);

        e.applyRating(5, "   ");

        assertEquals(5, e.getRating());
        assertEquals("keep", e.getComment());
        assertTrue(e.getUpdatedAt().isAfter(oldUpdated));
    }

    @Test
    void applyRatingWithNullCommentLeavesCommentNull() {
        OffsetDateTime created = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1);
        OffsetDateTime oldUpdated = created;
        WatchEntry e = new WatchEntry(1L, 1L, 2L, null, 2, null, LocalDate.now(), created, oldUpdated);

        e.applyRating(4, null);

        assertEquals(4, e.getRating());
        assertNull(e.getComment());
        assertTrue(e.getUpdatedAt().isAfter(oldUpdated));
    }
}