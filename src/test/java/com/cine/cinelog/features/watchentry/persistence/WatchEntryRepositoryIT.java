package com.cine.cinelog.features.watchentry.persistence;

import com.cine.cinelog.features.watchentry.persistence.entity.WatchEntryEntity;
import com.cine.cinelog.features.watchentry.repository.WatchEntryJpaRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class WatchEntryRepositoryIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.44")
            .withDatabaseName("cinelog")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", mysql::getJdbcUrl);
        r.add("spring.datasource.username", mysql::getUsername);
        r.add("spring.datasource.password", mysql::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.liquibase.change-log", () -> "classpath:liquibase/db.changelog-master.xml");
        r.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    WatchEntryJpaRepository repo;

    @Test
    void shouldSaveAndQueryByUserWithFilters() {
        // given
        var now = OffsetDateTime.now();
        var e = new WatchEntryEntity();
        e.setUserId(1L);
        e.setMediaId(10L);
        e.setEpisodeId(null);
        e.setRating(8);
        e.setComment("bom demais");
        e.setWatchedAt(LocalDate.of(2025, 10, 1));
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        var saved = repo.save(e);

        // when
        var page = repo.search(1L, 10L, null, 7, LocalDate.of(2025, 9, 1), LocalDate.of(2025, 12, 31),
                org.springframework.data.domain.PageRequest.of(0, 20));

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(page.getTotalElements()).isEqualTo(1);
        var got = page.getContent().get(0);
        assertThat(got.getUserId()).isEqualTo(1L);
        assertThat(got.getMediaId()).isEqualTo(10L);
        assertThat(got.getRating()).isEqualTo(8);
    }
}