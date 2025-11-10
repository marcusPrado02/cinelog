package com.cine.cinelog.core.domain.policy.impl;

import com.cine.cinelog.core.domain.error.DomainException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MaxSeasonsPolicyTest {

    @Test
    void of_withPositiveMax_createsPolicy() {
        MaxSeasonsPolicy policy = MaxSeasonsPolicy.of(3);
        assertNotNull(policy);
    }

    @Test
    void of_withZeroOrNegative_throwsDomainException() {
        DomainException ex0 = assertThrows(DomainException.class, () -> MaxSeasonsPolicy.of(0));
        assertTrue(ex0.getMessage().contains("max seasons inválido"));

        DomainException exNeg = assertThrows(DomainException.class, () -> MaxSeasonsPolicy.of(-5));
        assertTrue(exNeg.getMessage().contains("max seasons inválido"));
    }

    @Test
    void ensureWithinLimit_withinLimit_doesNotThrow() {
        MaxSeasonsPolicy policy = MaxSeasonsPolicy.of(2);
        assertDoesNotThrow(() -> policy.ensureWithinLimit(2));
        assertDoesNotThrow(() -> policy.ensureWithinLimit(0));
    }

    @Test
    void ensureWithinLimit_exceedsLimit_throwsDomainExceptionWithMessage() {
        MaxSeasonsPolicy policy = MaxSeasonsPolicy.of(2);
        DomainException ex = assertThrows(DomainException.class, () -> policy.ensureWithinLimit(3));
        assertTrue(ex.getMessage().contains("Série excede limite de temporadas"));
        assertTrue(ex.getMessage().contains("3 > 2"));
    }
}