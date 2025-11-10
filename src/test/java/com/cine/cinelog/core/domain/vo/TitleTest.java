package com.cine.cinelog.core.domain.vo;

import static org.junit.jupiter.api.Assertions.*;
import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;

class TitleTest {

    @Test
    void of_nullShouldThrowDomainExceptionWithRequiredMessage() {
        DomainException ex = assertThrows(DomainException.class, () -> Title.of(null));
        assertEquals("Título é obrigatório", ex.getMessage());
    }

    @Test
    void of_emptyShouldThrowDomainExceptionWithRequiredMessage() {
        DomainException ex = assertThrows(DomainException.class, () -> Title.of(""));
        assertEquals("Título é obrigatório", ex.getMessage());
    }

    @Test
    void of_blankShouldThrowDomainExceptionWithRequiredMessage() {
        DomainException ex = assertThrows(DomainException.class, () -> Title.of("   "));
        assertEquals("Título é obrigatório", ex.getMessage());
    }

    @Test
    void of_tooLongShouldThrowDomainExceptionWithTooLongMessage() {
        String tooLong = "a".repeat(201);
        DomainException ex = assertThrows(DomainException.class, () -> Title.of(tooLong));
        assertEquals("Título excede 200 caracteres", ex.getMessage());
    }

    @Test
    void of_validShouldCreateTitleWithTrimmedValue() {
        Title t = Title.of("  My Movie Title  ");
        assertEquals("My Movie Title", t.value());
        assertEquals("My Movie Title", t.toString());
    }

    @Test
    void equals_isCaseInsensitive() {
        Title a = Title.of("My Title");
        Title b = Title.of("my title");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void equals_sameInstanceAndValue() {
        Title a = Title.of("Unique");
        Title b = Title.of("Unique");
        assertEquals(a, b);
    }

    @Test
    void notEquals_differentValues() {
        Title a = Title.of("One");
        Title b = Title.of("Two");
        assertNotEquals(a, b);
    }
}