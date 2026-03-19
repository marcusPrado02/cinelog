package com.cine.cinelog.core.domain.vo;

import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

class RatingTest {

    @Test
    void shouldCreateRatingWhenValueIsValid() {
        Rating r = Rating.of(BigDecimal.valueOf(7.5));
        assertEquals(BigDecimal.valueOf(7.5), r.value());
    }

    @Test
    void shouldAllowBoundaryValuesZeroAndTen() {
        Rating r0 = Rating.of(BigDecimal.valueOf(0.0));
        Rating r10 = Rating.of(BigDecimal.valueOf(10.0));
        assertEquals(BigDecimal.valueOf(0.0), r0.value());
        assertEquals(BigDecimal.valueOf(10.0), r10.value());
    }

    @Test
    void ofShouldReturnZeroWhenNull() {
        Rating r = Rating.of(null);
        assertEquals(BigDecimal.valueOf(0.0), r.value());
    }

    @Test
    void shouldThrowWhenValueIsNaN() {
        // BigDecimal.valueOf(Double.NaN) throws NumberFormatException before Rating.of() is invoked,
        // because BigDecimal has no NaN concept. This is the expected low-level behaviour.
        assertThrows(NumberFormatException.class, () -> Rating.of(BigDecimal.valueOf(Double.NaN)));
    }

    @Test
    void shouldThrowWhenValueIsNegative() {
        DomainException ex = assertThrows(DomainException.class, () -> Rating.of(BigDecimal.valueOf(-0.1)));
        assertTrue(ex.getMessage().contains("Classificação fora do intervalo"));
    }

    @Test
    void shouldThrowWhenValueGreaterThanTen() {
        DomainException ex = assertThrows(DomainException.class, () -> Rating.of(BigDecimal.valueOf(10.1)));
        assertTrue(ex.getMessage().contains("Classificação fora do intervalo"));
    }
}
