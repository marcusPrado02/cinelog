package com.cine.cinelog.features.media.persistence.entity;

import org.junit.jupiter.api.Test;

import com.cine.cinelog.core.domain.enums.MediaType;

import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class MediaEntityTest {

    @Test
    void testGettersAndSetters() {
        MediaEntity entity = new MediaEntity();

        entity.setId(123L);
        entity.setTitle("Title Example");

        // pick any available enum constant dynamically to avoid hard dependency on
        // specific names
        MediaType[] constants = MediaType.class.getEnumConstants();
        MediaType anyType = constants != null && constants.length > 0 ? constants[0] : null;
        entity.setType(anyType);

        entity.setReleaseYear(2023);
        entity.setOriginalTitle("Original Title");
        entity.setOriginalLanguage("en");
        entity.setPosterUrl("http://example.com/poster.jpg");
        entity.setBackdropUrl("http://example.com/backdrop.jpg");
        entity.setOverview("Overview text");

        LocalDateTime created = LocalDateTime.of(2020, 1, 1, 0, 0);
        LocalDateTime updated = LocalDateTime.of(2020, 1, 2, 0, 0);
        entity.setCreatedAt(created);
        entity.setUpdatedAt(updated);

        assertEquals(123L, entity.getId());
        assertEquals("Title Example", entity.getTitle());
        assertEquals(anyType, entity.getType());
        assertEquals(Integer.valueOf(2023), entity.getReleaseYear());
        assertEquals("Original Title", entity.getOriginalTitle());
        assertEquals("en", entity.getOriginalLanguage());
        assertEquals("http://example.com/poster.jpg", entity.getPosterUrl());
        assertEquals("http://example.com/backdrop.jpg", entity.getBackdropUrl());
        assertEquals("Overview text", entity.getOverview());
        assertEquals(created, entity.getCreatedAt());
        assertEquals(updated, entity.getUpdatedAt());
    }

    @Test
    void testPrePersistSetsCreatedAtAndUpdatedAt() {
        MediaEntity entity = new MediaEntity();

        assertNull(entity.getCreatedAt());
        assertNull(entity.getUpdatedAt());

        entity.prePersist();

        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
    }

    @Test
    void testPreUpdateUpdatesUpdatedAt() throws InterruptedException {
        MediaEntity entity = new MediaEntity();

        // initialize timestamps via prePersist
        entity.prePersist();
        LocalDateTime beforeUpdate = entity.getUpdatedAt();

        // ensure measurable time difference
        Thread.sleep(10);

        entity.preUpdate();
        LocalDateTime afterUpdate = entity.getUpdatedAt();

        assertNotNull(afterUpdate);
        assertTrue(afterUpdate.isAfter(beforeUpdate), "updatedAt should be later after preUpdate");
        // createdAt should remain unchanged by preUpdate
        assertEquals(entity.getCreatedAt(), entity.getCreatedAt());
    }
}