package com.cine.cinelog.features.credits.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class CreditEntityTest {

    @Test
    void defaultValuesShouldBeNull() {
        CreditEntity e = new CreditEntity();
        assertNull(e.getId());
        assertNull(e.getMediaId());
        assertNull(e.getPersonId());
        assertNull(e.getRole());
        assertNull(e.getCharacterName());
        assertNull(e.getOrderIndex());
    }

    @Test
    void settersAndGettersShouldWork() {
        CreditEntity e = new CreditEntity();

        e.setId(1L);
        e.setMediaId(10L);
        e.setPersonId(20L);
        e.setRole("ACTOR");
        e.setCharacterName("John Doe");
        e.setOrderIndex(Short.valueOf((short) 5));

        assertEquals(1L, e.getId());
        assertEquals(10L, e.getMediaId());
        assertEquals(20L, e.getPersonId());
        assertEquals("ACTOR", e.getRole());
        assertEquals("John Doe", e.getCharacterName());
        assertEquals(Short.valueOf((short) 5), e.getOrderIndex());
    }

    @Test
    void jpaAnnotationsShouldBePresentAndConfigured() throws NoSuchFieldException {
        Class<CreditEntity> clazz = CreditEntity.class;

        Table table = clazz.getAnnotation(Table.class);
        assertNotNull(table);
        assertEquals("credits", table.name());
        Index[] indexes = table.indexes();
        assertEquals(2, indexes.length);
        // verify expected column lists exist in any order
        boolean hasMediaRole = false;
        boolean hasPersonRole = false;
        for (Index idx : indexes) {
            String colList = idx.columnList().replaceAll("\\s+", "");
            if ("media_id,role".equals(colList))
                hasMediaRole = true;
            if ("person_id,role".equals(colList))
                hasPersonRole = true;
        }
        assertTrue(hasMediaRole, "missing index on media_id, role");
        assertTrue(hasPersonRole, "missing index on person_id, role");

        Field idField = clazz.getDeclaredField("id");
        assertNotNull(idField.getAnnotation(Id.class));
        assertNotNull(idField.getAnnotation(GeneratedValue.class));

        Field mediaField = clazz.getDeclaredField("mediaId");
        Column mediaCol = mediaField.getAnnotation(Column.class);
        assertNotNull(mediaCol);
        assertEquals("media_id", mediaCol.name());
        assertFalse(mediaCol.nullable());

        Field personField = clazz.getDeclaredField("personId");
        Column personCol = personField.getAnnotation(Column.class);
        assertNotNull(personCol);
        assertEquals("person_id", personCol.name());
        assertFalse(personCol.nullable());

        Field roleField = clazz.getDeclaredField("role");
        Column roleCol = roleField.getAnnotation(Column.class);
        assertNotNull(roleCol);
        assertFalse(roleCol.nullable());
        assertEquals("ENUM('DIRECTOR','WRITER','ACTOR','PRODUCER','COMPOSER')", roleCol.columnDefinition());

        Field charField = clazz.getDeclaredField("characterName");
        Column charCol = charField.getAnnotation(Column.class);
        assertNotNull(charCol);
        assertEquals(200, charCol.length());
        assertEquals("character_name", charCol.name());
    }
}