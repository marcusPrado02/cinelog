package com.cine.cinelog.features.users.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class UserEntityTest {

    @Test
    void gettersAndSettersShouldWork() {
        UserEntity u = new UserEntity();
        Long id = 42L;
        String name = "John Doe";
        String email = "john.doe@example.com";
        OffsetDateTime now = OffsetDateTime.parse("2023-01-02T03:04:05Z");

        u.setId(id);
        u.setName(name);
        u.setEmail(email);
        u.setCreatedAt(now);

        assertEquals(id, u.getId());
        assertEquals(name, u.getName());
        assertEquals(email, u.getEmail());
        assertEquals(now, u.getCreatedAt());
    }

    @Test
    void prePersistShouldSetCreatedAtWhenNull() {
        UserEntity u = new UserEntity();
        assertNull(u.getCreatedAt(), "createdAt should start null");

        u.pre();

        assertNotNull(u.getCreatedAt(), "pre() should set createdAt when null");
    }

    @Test
    void prePersistShouldNotOverrideExistingCreatedAt() {
        UserEntity u = new UserEntity();
        OffsetDateTime fixed = OffsetDateTime.parse("2020-01-01T10:00:00Z");
        u.setCreatedAt(fixed);

        u.pre();

        assertEquals(fixed, u.getCreatedAt(), "pre() should not override an existing createdAt");
    }

    @Test
    void tableAndUniqueConstraintAnnotationsPresent() {
        Table table = UserEntity.class.getAnnotation(Table.class);
        assertNotNull(table, "UserEntity should have @Table");
        assertEquals("users", table.name(), "table name should be 'users'");

        UniqueConstraint[] ucs = table.uniqueConstraints();
        assertTrue(ucs.length > 0, "uniqueConstraints should be present");

        boolean found = Arrays.stream(ucs).anyMatch(
                uc -> "uk_users_email".equals(uc.name()) && Arrays.equals(new String[] { "email" }, uc.columnNames()));
        assertTrue(found, "should contain unique constraint 'uk_users_email' on column 'email'");
    }

    @Test
    void columnAnnotationsOnFields() throws NoSuchFieldException {
        Field nameField = UserEntity.class.getDeclaredField("name");
        Column nameCol = nameField.getAnnotation(Column.class);
        assertNotNull(nameCol, "name field should have @Column");
        assertFalse(nameCol.nullable(), "name should be not nullable");
        assertEquals(120, nameCol.length(), "name length should be 120");

        Field emailField = UserEntity.class.getDeclaredField("email");
        Column emailCol = emailField.getAnnotation(Column.class);
        assertNotNull(emailCol, "email field should have @Column");
        assertFalse(emailCol.nullable(), "email should be not nullable");
        assertEquals(255, emailCol.length(), "email length should be 255");

        Field createdAtField = UserEntity.class.getDeclaredField("createdAt");
        Column createdAtCol = createdAtField.getAnnotation(Column.class);
        assertNotNull(createdAtCol, "createdAt field should have @Column");
        assertEquals("created_at", createdAtCol.name(), "createdAt column name should be 'created_at'");
        assertFalse(createdAtCol.nullable(), "createdAt should be not nullable");
    }
}