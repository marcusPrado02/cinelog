package com.cine.cinelog.features.batch.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BatchJobsConfigTest {

    @BeforeEach
    void setUp() {
        // Tests use reflection and direct instantiation — no Spring context needed
    }

    static Stream<Arguments> allJobNames() {
        return Stream.of(
                Arguments.of("syncGenresJob"),
                Arguments.of("importMoviesJob"),
                Arguments.of("importTvShowsJob"),
                Arguments.of("importCreditsJob"),
                Arguments.of("importSeasonsJob"),
                Arguments.of("syncReviewsJob"),
                Arguments.of("enrichMediaImagesJob"),
                Arguments.of("enrichPersonProfilesJob")
        );
    }

    static Stream<Arguments> allStepMethodNames() {
        return Stream.of(
                Arguments.of("syncGenresStep"),
                Arguments.of("importMoviesStep"),
                Arguments.of("importTvShowsStep"),
                Arguments.of("importCreditsStep"),
                Arguments.of("importSeasonsStep"),
                Arguments.of("syncReviewsStep"),
                Arguments.of("enrichMediaImagesStep"),
                Arguments.of("enrichPersonProfilesStep")
        );
    }

    @Test
    @DisplayName("should have exactly 8 Job @Bean methods")
    void shouldHaveEightJobBeans() {
        long jobBeanCount = Arrays.stream(BatchJobsConfig.class.getDeclaredMethods())
                .filter(m -> m.getReturnType() == Job.class)
                .filter(m -> m.isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .count();

        assertThat(jobBeanCount).isEqualTo(8);
    }

    @Test
    @DisplayName("should have exactly 8 Step @Bean methods")
    void shouldHaveEightStepBeans() {
        long stepBeanCount = Arrays.stream(BatchJobsConfig.class.getDeclaredMethods())
                .filter(m -> m.getReturnType() == Step.class)
                .filter(m -> m.isAnnotationPresent(org.springframework.context.annotation.Bean.class))
                .count();

        assertThat(stepBeanCount).isEqualTo(8);
    }

    @ParameterizedTest(name = "Job method {0} should exist and return Job")
    @MethodSource("allJobNames")
    @DisplayName("should declare all expected Job beans")
    void shouldDeclareAllJobBeans(String methodName) {
        Method method = Arrays.stream(BatchJobsConfig.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertThat(method)
                .as("Expected @Bean method '%s' to exist", methodName)
                .isNotNull();
        assertThat(method.getReturnType()).isEqualTo(Job.class);
    }

    @ParameterizedTest(name = "Step method {0} should exist and return Step")
    @MethodSource("allStepMethodNames")
    @DisplayName("should declare all expected Step beans")
    void shouldDeclareAllStepBeans(String methodName) {
        Method method = Arrays.stream(BatchJobsConfig.class.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);

        assertThat(method)
                .as("Expected @Bean method '%s' to exist", methodName)
                .isNotNull();
        assertThat(method.getReturnType()).isEqualTo(Step.class);
    }

    @Test
    @DisplayName("BatchJobProperties should have correct defaults")
    void shouldHaveCorrectDefaults() {
        BatchJobProperties defaults = new BatchJobProperties();

        assertThat(defaults.getMaxPages()).isEqualTo(10);
        assertThat(defaults.getChunkSize()).isEqualTo(20);
        assertThat(defaults.getSortBy()).isEqualTo("popularity.desc");
        assertThat(defaults.getJobs()).isNotNull();
    }

    @Test
    @DisplayName("all JobConfig defaults should have enabled=true")
    void allJobConfigsShouldBeEnabledByDefault() {
        BatchJobProperties.Jobs jobs = new BatchJobProperties.Jobs();

        List<BatchJobProperties.JobConfig> configs = List.of(
                jobs.getSyncGenres(),
                jobs.getImportMovies(),
                jobs.getImportTvShows(),
                jobs.getImportCredits(),
                jobs.getImportSeasons(),
                jobs.getSyncReviews(),
                jobs.getEnrichMediaImages(),
                jobs.getEnrichPersonProfiles()
        );

        assertThat(configs).allMatch(BatchJobProperties.JobConfig::isEnabled,
                "All job configs should be enabled by default");
    }

    @Test
    @DisplayName("all JobConfig defaults should have a cron expression")
    void allJobConfigsShouldHaveCronExpression() {
        BatchJobProperties.Jobs jobs = new BatchJobProperties.Jobs();

        List<BatchJobProperties.JobConfig> configs = List.of(
                jobs.getSyncGenres(),
                jobs.getImportMovies(),
                jobs.getImportTvShows(),
                jobs.getImportCredits(),
                jobs.getImportSeasons(),
                jobs.getSyncReviews(),
                jobs.getEnrichMediaImages(),
                jobs.getEnrichPersonProfiles()
        );

        assertThat(configs)
                .extracting(BatchJobProperties.JobConfig::getCron)
                .allMatch(cron -> cron != null && cron.contains("*"),
                        "All job configs should have a valid cron expression");
    }

    @Test
    @DisplayName("JobConfig should support constructor and getters")
    void jobConfigConstructorAndGetters() {
        BatchJobProperties.JobConfig jobConfig = new BatchJobProperties.JobConfig("0 0 1 * * *", false, 5);

        assertThat(jobConfig.getCron()).isEqualTo("0 0 1 * * *");
        assertThat(jobConfig.isEnabled()).isFalse();
        assertThat(jobConfig.getMaxPages()).isEqualTo(5);
    }

    @Test
    @DisplayName("JobConfig setters should update values")
    void jobConfigSetters() {
        BatchJobProperties.JobConfig jobConfig = new BatchJobProperties.JobConfig();
        jobConfig.setCron("0 0 2 * * *");
        jobConfig.setEnabled(true);
        jobConfig.setMaxPages(15);

        assertThat(jobConfig.getCron()).isEqualTo("0 0 2 * * *");
        assertThat(jobConfig.isEnabled()).isTrue();
        assertThat(jobConfig.getMaxPages()).isEqualTo(15);
    }
}
