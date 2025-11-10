package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RatingTest {

    @Test
    void shouldCreateRatingWhenValueIsValid() {
        Rating r = new Rating(7.5);
        assertEquals(7.5, r.value(), 0.0);
    }

    @Test
    void shouldAllowBoundaryValuesZeroAndTen() {
        Rating r0 = new Rating(0.0);
        Rating r10 = new Rating(10.0);
        assertEquals(0.0, r0.value(), 0.0);
        assertEquals(10.0, r10.value(), 0.0);
    }

    @Test
    void ofShouldReturnZeroWhenNull() {
        Rating r = Rating.of(null);
        assertEquals(0.0, r.value(), 0.0);
    }

    @Test
    void shouldThrowWhenValueIsNaN() {
        DomainException ex = assertThrows(DomainException.class, () -> new Rating(Double.NaN));
        assertEquals("Rating deve estar entre 0 e 10", ex.getMessage());
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        DomainException ex = assertThrows(DomainException.class, () -> new Rating(-0.1));
        assertEquals("Rating deve estar entre 0 e 10", ex.getMessage());
    }

    @Test
    void shouldThrowWhenValueGreaterThanTen() {
        DomainException ex = assertThrows(DomainException.class, () -> new Rating(10.1));
        assertEquals("Rating deve estar entre 0 e 10", ex.getMessage());
    }
}