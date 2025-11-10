package com.cine.cinelog.features.seasons.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class SeasonEntityTest {

    @Test
    void testGettersAndSetters() {
        SeasonEntity s = new SeasonEntity();
        s.setId(10L);
        s.setMediaId(20L);
        s.setSeasonNumber(3);
        s.setName("Season Three");
        LocalDate date = LocalDate.of(2021, 6, 15);
        s.setAirDate(date);

        assertEquals(10L, s.getId());
        assertEquals(20L, s.getMediaId());
        assertEquals(3, s.getSeasonNumber());
        assertEquals("Season Three", s.getName());
        assertEquals(date, s.getAirDate());
    }

    @Test
    void testDefaultsAreNull() {
        SeasonEntity s = new SeasonEntity();
        assertNull(s.getId());
        assertNull(s.getMediaId());
        assertNull(s.getSeasonNumber());
        assertNull(s.getName());
        assertNull(s.getAirDate());
    }

    @Test
    void testJpaAnnotationsPresentAndConfigured() throws NoSuchFieldException {
        Class<SeasonEntity> clazz = SeasonEntity.class;

        // Class-level annotations
        assertTrue(clazz.isAnnotationPresent(Entity.class), "Missing @Entity");
        Table table = clazz.getAnnotation(Table.class);
        assertNotNull(table, "Missing @Table");
        assertEquals("seasons", table.name());
        UniqueConstraint[] ucs = table.uniqueConstraints();
        assertEquals(1, ucs.length);
        UniqueConstraint uc = ucs[0];
        assertEquals("uk_season_media_number", uc.name());
        assertArrayEquals(new String[] { "media_id", "season_number" }, uc.columnNames());

        // id field
        Field idField = clazz.getDeclaredField("id");
        assertTrue(idField.isAnnotationPresent(Id.class));
        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(gv);
        assertEquals(GenerationType.IDENTITY, gv.strategy());

        // mediaId field
        Field mediaField = clazz.getDeclaredField("mediaId");
        Column mediaCol = mediaField.getAnnotation(Column.class);
        assertNotNull(mediaCol);
        assertEquals("media_id", mediaCol.name());
        assertFalse(mediaCol.nullable());

        // seasonNumber field
        Field seasonField = clazz.getDeclaredField("seasonNumber");
        Column seasonCol = seasonField.getAnnotation(Column.class);
        assertNotNull(seasonCol);
        assertEquals("season_number", seasonCol.name());
        assertFalse(seasonCol.nullable());

        // name field
        Field nameField = clazz.getDeclaredField("name");
        Column nameCol = nameField.getAnnotation(Column.class);
        assertNotNull(nameCol);
        assertEquals(200, nameCol.length());

        // airDate field
        Field airField = clazz.getDeclaredField("airDate");
        Column airCol = airField.getAnnotation(Column.class);
        assertNotNull(airCol);
        assertEquals("air_date", airCol.name());
    }
}