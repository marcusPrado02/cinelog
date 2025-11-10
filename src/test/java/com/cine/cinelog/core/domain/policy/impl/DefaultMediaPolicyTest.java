package com.cine.cinelog.core.domain.policy.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Year;
import org.junit.jupiter.api.Test;
import com.cine.cinelog.core.domain.error.DomainException;
import com.cine.cinelog.core.domain.model.Media;

class DefaultMediaPolicyTest {

    private static final int MIN_YEAR = 1888;
    private static final int FUTURE_SLACK = 1;

    @Test
    void shouldThrowWhenMediaIsNull() {
        DefaultMediaPolicy policy = new DefaultMediaPolicy(MIN_YEAR, FUTURE_SLACK);

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateInvariants(null));
        assertEquals("media must not be null", ex.getMessage());
    }

    @Test
    void shouldNotThrowWhenReleaseYearIsNull() {
        DefaultMediaPolicy policy = new DefaultMediaPolicy(MIN_YEAR, FUTURE_SLACK);
        Media media = mock(Media.class);
        when(media.getReleaseYear()).thenReturn(null);

        assertDoesNotThrow(() -> policy.validateInvariants(media));
    }

    @Test
    void shouldAllowYearsWithinBounds() {
        int current = Year.now().getValue();
        int maxYear = current + FUTURE_SLACK;

        DefaultMediaPolicy policy = new DefaultMediaPolicy(MIN_YEAR, FUTURE_SLACK);

        Media mediaAtMin = mock(Media.class);
        when(mediaAtMin.getReleaseYear()).thenReturn(MIN_YEAR);
        assertDoesNotThrow(() -> policy.validateInvariants(mediaAtMin));

        Media mediaAtCurrent = mock(Media.class);
        when(mediaAtCurrent.getReleaseYear()).thenReturn(current);
        assertDoesNotThrow(() -> policy.validateInvariants(mediaAtCurrent));

        Media mediaAtMax = mock(Media.class);
        when(mediaAtMax.getReleaseYear()).thenReturn(maxYear);
        assertDoesNotThrow(() -> policy.validateInvariants(mediaAtMax));
    }

    @Test
    void shouldThrowWhenYearBelowMin() {
        DefaultMediaPolicy policy = new DefaultMediaPolicy(MIN_YEAR, FUTURE_SLACK);

        Media media = mock(Media.class);
        when(media.getReleaseYear()).thenReturn(MIN_YEAR - 1);

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateInvariants(media));
        assertEquals("invalid release year", ex.getMessage());
    }

    @Test
    void shouldThrowWhenYearAboveMax() {
        int current = Year.now().getValue();
        int maxYear = current + FUTURE_SLACK;

        DefaultMediaPolicy policy = new DefaultMediaPolicy(MIN_YEAR, FUTURE_SLACK);

        Media media = mock(Media.class);
        when(media.getReleaseYear()).thenReturn(maxYear + 1);

        DomainException ex = assertThrows(DomainException.class, () -> policy.validateInvariants(media));
        assertEquals("invalid release year", ex.getMessage());
    }
}