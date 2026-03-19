package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class YearTest {

    @Test
    void shouldCreateForValidYear() {
        int y = 2000;
        Year year = Year.of(y);
        assertNotNull(year);
        assertEquals(y, year.value());
    }

    @Test
    void shouldAllowBoundaryValues() {
        int current = java.time.Year.now().getValue();
        Year low = Year.of(1888);
        Year high = Year.of(current + 2);
        assertEquals(1888, low.value());
        assertEquals(current + 2, high.value());
    }

    @Test
    void shouldThrowWhenYearIsNull() {
        DomainException ex = assertThrows(DomainException.class, () -> Year.of(null));
        // GEN_VALIDATION title is thrown when null
        assertNotNull(ex.getMessage());
    }

    @Test
    void shouldThrowWhenYearBeforeMinimum() {
        int invalid = 1887;
        DomainException ex = assertThrows(DomainException.class, () -> Year.of(invalid));
        // MEDIA_YEAR_OUT_OF_RANGE title is thrown for out-of-range years
        assertTrue(ex.getMessage().contains("Ano fora do intervalo"));
    }

    @Test
    void shouldThrowWhenYearAfterMaximum() {
        int current = java.time.Year.now().getValue();
        int invalid = current + 3;
        DomainException ex = assertThrows(DomainException.class, () -> Year.of(invalid));
        // MEDIA_YEAR_OUT_OF_RANGE title is thrown for out-of-range years
        assertTrue(ex.getMessage().contains("Ano fora do intervalo"));
    }
}
