package com.cine.cinelog.core.domain.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.assertj.core.api.Assertions.*;

@DisplayName("SeriesProgress Value Object Tests")
class SeriesProgressTest {

    @Test
    @DisplayName("Should create SeriesProgress with valid values")
    void shouldCreateSeriesProgressWithValidValues() {
        SeriesProgress progress = new SeriesProgress(5, 12, Duration.ofMinutes(25), Duration.ofMinutes(45));
        assertThat(progress.getCurrentSeason()).isEqualTo(5);
        assertThat(progress.getCurrentEpisode()).isEqualTo(12);
    }

    @Test
    @DisplayName("Should calculate completion percentage")
    void shouldCalculateCompletionPercentage() {
        SeriesProgress progress = new SeriesProgress(1, 1, Duration.ofMinutes(30), Duration.ofMinutes(60));
        double percentage = progress.getCompletionPercentage();
        assertThat(percentage).isEqualTo(50.0);
    }

    @Test
    @DisplayName("Should throw exception when season is zero")
    void shouldThrowWhenSeasonZero() {
        assertThatThrownBy(() -> new SeriesProgress(0, 1, Duration.ZERO, Duration.ofMinutes(45)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should be equal when all fields match")
    void shouldBeEqual() {
        SeriesProgress p1 = new SeriesProgress(3, 8, Duration.ofMinutes(25), Duration.ofMinutes(45));
        SeriesProgress p2 = new SeriesProgress(3, 8, Duration.ofMinutes(25), Duration.ofMinutes(45));
        assertThat(p1).isEqualTo(p2);
    }
}
