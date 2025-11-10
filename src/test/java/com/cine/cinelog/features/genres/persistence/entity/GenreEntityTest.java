package com.cine.cinelog.features.genres.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class GenreEntityTest {

    @Test
    void gettersAndSetters_workAsExpected() {
        GenreEntity entity = new GenreEntity();
        assertNull(entity.getId(), "id should be null by default");
        assertNull(entity.getName(), "name should be null by default");

        Long expectedId = 5L;
        String expectedName = "Action";

        entity.setId(expectedId);
        entity.setName(expectedName);

        assertEquals(expectedId, entity.getId());
        assertEquals(expectedName, entity.getName());
    }

    @Test
    void classHasEntityAndTableAnnotations_withExpectedValues() {
        Class<GenreEntity> clazz = GenreEntity.class;

        assertTrue(clazz.isAnnotationPresent(Entity.class), "@Entity should be present");

        assertTrue(clazz.isAnnotationPresent(Table.class), "@Table should be present");
        Table table = clazz.getAnnotation(Table.class);
        assertEquals("genres", table.name(), "table name should be 'genres'");

        UniqueConstraint[] uniques = table.uniqueConstraints();
        assertNotNull(uniques, "uniqueConstraints should not be null");
        assertTrue(uniques.length > 0, "there should be at least one unique constraint");

        // find constraint named uk_genres_name and ensure it references column "name"
        boolean found = false;
        for (UniqueConstraint uc : uniques) {
            if ("uk_genres_name".equals(uc.name())) {
                String[] cols = uc.columnNames();
                assertNotNull(cols);
                assertEquals(1, cols.length);
                assertEquals("name", cols[0]);
                found = true;
                break;
            }
        }
        assertTrue(found, "unique constraint named 'uk_genres_name' should be present");
    }

    @Test
    void idField_hasIdAndGeneratedValueAnnotations_withIdentityStrategy() throws NoSuchFieldException {
        Field idField = GenreEntity.class.getDeclaredField("id");

        assertTrue(idField.isAnnotationPresent(Id.class), "id field should have @Id");
        assertTrue(idField.isAnnotationPresent(GeneratedValue.class), "id field should have @GeneratedValue");

        GeneratedValue gv = idField.getAnnotation(GeneratedValue.class);
        assertEquals(GenerationType.IDENTITY, gv.strategy(), "GeneratedValue strategy should be IDENTITY");

        assertEquals(Short.class, idField.getType(), "id field should be of type Short");
    }

    @Test
    void nameField_hasColumnAnnotation_withNotNullAndLength100() throws NoSuchFieldException {
        Field nameField = GenreEntity.class.getDeclaredField("name");

        assertTrue(nameField.isAnnotationPresent(Column.class), "name field should have @Column");
        Column col = nameField.getAnnotation(Column.class);
        assertFalse(col.nullable(), "Column.nullable should be false");
        assertEquals(100, col.length(), "Column.length should be 100");

        assertEquals(String.class, nameField.getType(), "name field should be of type String");
    }
}