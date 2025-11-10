package com.cine.cinelog.core.domain.spec;

import com.cine.cinelog.core.domain.model.Media;
import com.cine.cinelog.core.domain.vo.Rating;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;

class IsPopularMediaSpecificationTest {

    private final IsPopularMediaSpecification spec = new IsPopularMediaSpecification();
    private final int currentYear = java.time.Year.now().getValue();

    @Test
    void returnsTrueWhenMediaIsRecentAndRatingIsAtLeastSeven() {
        Media media = Mockito.mock(Media.class);
        Mockito.when(media.getReleaseYear()).thenReturn(currentYear); // recent
        boolean result = spec.isSatisfiedBy(media, 7.0);
        assertTrue(result);
    }

    @Test
    void returnsTrueWhenMediaIsExactlyTenYearsOldAndRatingIsSeven() {
        Media media = Mockito.mock(Media.class);
        Mockito.when(media.getReleaseYear()).thenReturn(currentYear - 10); // boundary recent
        boolean result = spec.isSatisfiedBy(media, 7.0);
        assertTrue(result);
    }

    @Test
    void returnsFalseWhenMediaIsRecentButRatingIsBelowSeven() {
        Media media = Mockito.mock(Media.class);
        Mockito.when(media.getReleaseYear()).thenReturn(currentYear - 1); // recent
        boolean result = spec.isSatisfiedBy(media, 6.9);
        assertFalse(result);
    }

    @Test
    void returnsFalseWhenMediaIsOlderThanTenYearsEvenIfRatingIsHigh() {
        Media media = Mockito.mock(Media.class);
        Mockito.when(media.getReleaseYear()).thenReturn(currentYear - 11); // not recent
        boolean result = spec.isSatisfiedBy(media, 9.5);
        assertFalse(result);
    }

    @Test
    void overloadAcceptsRatingValueObject() {
        Media media = Mockito.mock(Media.class);
        Rating rating = Mockito.mock(Rating.class);
        Mockito.when(media.getReleaseYear()).thenReturn(currentYear); // recent
        Mockito.when(rating.value()).thenReturn(8.2);
        boolean result = spec.isSatisfiedBy(media, rating);
        assertTrue(result);
    }
}