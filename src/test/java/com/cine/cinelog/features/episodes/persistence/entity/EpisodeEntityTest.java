package com.cine.cinelog.features.episodes.persistence.entity;

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

class EpisodeEntityTest {

    @Test
    void gettersAndSetters_shouldAllowReadAndWriteFields() {
        EpisodeEntity e = new EpisodeEntity();

        e.setId(10L);
        e.setSeasonId(2L);
        e.setEpisodeNumber(5);
        e.setName("Pilot");
        LocalDate date = LocalDate.of(2020, 1, 15);
        e.setAirDate(date);

        assertEquals(10L, e.getId());
        assertEquals(2L, e.getSeasonId());
        assertEquals(5, e.getEpisodeNumber());
        assertEquals("Pilot", e.getName());
        assertEquals(date, e.getAirDate());
    }

    @Test
    void newInstance_shouldHaveNullFieldsByDefault() {
        EpisodeEntity e = new EpisodeEntity();

        assertNull(e.getId());
        assertNull(e.getSeasonId());
        assertNull(e.getEpisodeNumber());
        assertNull(e.getName());
        assertNull(e.getAirDate());
    }

    @Test
    void classShouldBeAnnotatedWithEntityAndTable_withExpectedValues() {
        Class<EpisodeEntity> cls = EpisodeEntity.class;

        Entity entityAnn = cls.getAnnotation(Entity.class);
        assertNotNull(entityAnn, "Missing @Entity on EpisodeEntity");

        Table tableAnn = cls.getAnnotation(Table.class);
        assertNotNull(tableAnn, "Missing @Table on EpisodeEntity");
        assertEquals("episodes", tableAnn.name());

        UniqueConstraint[] ucs = tableAnn.uniqueConstraints();
        assertNotNull(ucs);
        assertTrue(ucs.length >= 1, "Expected at least one unique constraint");
        boolean found = false;
        for (UniqueConstraint uc : ucs) {
            if ("uk_episode_season_number".equals(uc.name())) {
                String[] cols = uc.columnNames();
                assertArrayEquals(new String[] { "season_id", "episode_number" }, cols);
                found = true;
            }
        }
        assertTrue(found, "Expected unique constraint named uk_episode_season_number");
    }

    @Test
    void fieldsShouldHaveExpectedColumnAnnotations() throws NoSuchFieldException {
        Field idField = EpisodeEntity.class.getDeclaredField("id");
        assertNotNull(idField.getAnnotation(Id.class), "id field must be annotated with @Id");

        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertNotNull(gv, "id field must be annotated with @GeneratedValue");
        assertEquals(GenerationType.IDENTITY, gv.strategy());

        Field seasonId = EpisodeEntity.class.getDeclaredField("seasonId");
        Column seasonCol = seasonId.getAnnotation(Column.class);
        assertNotNull(seasonCol);
        assertEquals("season_id", seasonCol.name());
        assertFalse(seasonCol.nullable());

        Field episodeNumber = EpisodeEntity.class.getDeclaredField("episodeNumber");
        Column epCol = episodeNumber.getAnnotation(Column.class);
        assertNotNull(epCol);
        assertEquals("episode_number", epCol.name());
        assertFalse(epCol.nullable());

        Field name = EpisodeEntity.class.getDeclaredField("name");
        Column nameCol = name.getAnnotation(Column.class);
        assertNotNull(nameCol);
        assertEquals(200, nameCol.length());

        Field airDate = EpisodeEntity.class.getDeclaredField("airDate");
        Column airCol = airDate.getAnnotation(Column.class);
        assertNotNull(airCol);
        assertEquals("air_date", airCol.name());
    }
}