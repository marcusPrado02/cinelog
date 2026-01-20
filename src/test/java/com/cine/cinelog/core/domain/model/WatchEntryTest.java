package com.cine.cinelog.core.domain.model;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class WatchEntryTest {

    @Test
    void constructor_requiresUserId() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () -> new WatchEntry(
                1L, null, 10L, null, null, null, LocalDate.now(), now, now, null, null, null));
    }

    @Test
    void constructor_requiresMediaOrEpisode() {
        LocalDateTime now = LocalDateTime.now();
        assertThrows(IllegalArgumentException.class, () -> new WatchEntry(
                1L, 2L, null, null, null, null, LocalDate.now(), now, now, null, null, null));
    }

    @Test
    void constructor_rejectsRatingOutOfRange_and_acceptsBounds() {
        LocalDateTime now = LocalDateTime.now();
        // negative rating
        assertThrows(IllegalArgumentException.class, () -> new WatchEntry(
                1L, 2L, 3L, null, new BigDecimal("-1"), null, LocalDate.now(), now, now, null, null, null));
        // greater than 10
        assertThrows(IllegalArgumentException.class, () -> new WatchEntry(
                1L, 2L, 3L, null, new BigDecimal("10.1"), null, LocalDate.now(), now, now, null, null, null));

        // boundaries 0 and 10 are allowed
        WatchEntry zero = new WatchEntry(1L, 2L, 3L, null, BigDecimal.ZERO, null, LocalDate.now(), now, now, null, null,
                null);
        WatchEntry ten = new WatchEntry(2L, 2L, 3L, null, BigDecimal.TEN, null, LocalDate.now(), now, now, null, null,
                null);
        assertEquals(BigDecimal.ZERO, zero.getRating());
        assertEquals(BigDecimal.TEN, ten.getRating());
    }

    @Test
    void withId_returnsNewInstanceWithGivenId() {
        LocalDateTime now = LocalDateTime.now();
        WatchEntry original = new WatchEntry(1L, 2L, 3L, null, BigDecimal.ONE, "c", LocalDate.now(), now, now, null,
                null, null);
        WatchEntry updated = original.withId(99L);

        assertEquals(1L, original.getId());
        assertEquals(99L, updated.getId());
        // other fields preserved
        assertEquals(original.getUserId(), updated.getUserId());
        assertEquals(original.getMediaId(), updated.getMediaId());
        assertEquals(original.getRating(), updated.getRating());
        // because equals is based on id only, they should not be equal
        assertNotEquals(original, updated);
    }

    @Test
    void equalsAndHashCode_basedOnlyOnId() {
        LocalDateTime now = LocalDateTime.now();
        WatchEntry a = new WatchEntry(5L, 10L, 20L, null, BigDecimal.ONE, "a", LocalDate.now(), now, now, null, null,
                null);
        WatchEntry b = new WatchEntry(5L, 99L, null, 200L, BigDecimal.TEN, "different", LocalDate.now(), now, now, null,
                null, null);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        WatchEntry c = new WatchEntry(6L, 10L, 20L, null, BigDecimal.ONE, "a", LocalDate.now(), now, now, null, null,
                null);
        assertNotEquals(a, c);
    }

    @Test
    void applyRating_setsRating_trimsComment_and_updatesUpdatedAt() {
        LocalDateTime created = LocalDateTime.now().minusDays(2);
        LocalDateTime previousUpdated = LocalDateTime.now().minusHours(1);
        WatchEntry w = new WatchEntry(1L, 2L, 3L, null, null, "old", LocalDate.now(), created, previousUpdated, null,
                null, null);

        LocalDateTime before = LocalDateTime.now();
        w.applyRating(new BigDecimal("7"), "  Nice show  ");
        assertEquals(new BigDecimal("7"), w.getRating());
        assertEquals("Nice show", w.getComment());
        assertNotNull(w.getUpdatedAt());
        assertTrue(!w.getUpdatedAt().isBefore(before));
        assertTrue(w.getUpdatedAt().isAfter(previousUpdated) || w.getUpdatedAt().isEqual(previousUpdated) == false);
    }

    @Test
    void applyRating_withNullOrBlankComment_keepsExistingComment() {
        LocalDateTime created = LocalDateTime.now().minusDays(2);
        LocalDateTime previousUpdated = LocalDateTime.now().minusHours(2);
        WatchEntry w = new WatchEntry(1L, 2L, 3L, null, BigDecimal.ONE, "keep me", LocalDate.now(), created,
                previousUpdated, null, null, null);

        w.applyRating(null, "   "); // blank comment -> should keep existing
        assertNull(w.getRating());
        assertEquals("keep me", w.getComment());
        assertNotNull(w.getUpdatedAt());
        assertTrue(w.getUpdatedAt().isAfter(previousUpdated));
    }
}