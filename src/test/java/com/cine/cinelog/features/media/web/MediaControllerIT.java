package com.cine.cinelog.features.media.web;

import com.cine.cinelog.core.domain.enums.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class MediaControllerIT {

        @Container
        static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0.44")
                        .withDatabaseName("cinelog")
                        .withUsername("cinelog")
                        .withPassword("cinelog")
                        .withInitScript("test-init.sql"); // cria schema audit e grants

        @DynamicPropertySource
        static void datasourceProps(DynamicPropertyRegistry registry) {
                registry.add("spring.datasource.url", mysql::getJdbcUrl);
                registry.add("spring.datasource.username", mysql::getUsername);
                registry.add("spring.datasource.password", mysql::getPassword);
                registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
                registry.add("spring.liquibase.enabled", () -> "true");
                // se você quiser pular algum context do Liquibase, pode usar:
                // registry.add("spring.liquibase.contexts", () -> "base"); // e marcar seus
                // changeSets com context="audit" para pular
        }

        @Autowired
        MockMvc mvc;
        @Autowired
        ObjectMapper mapper;

        Map<String, Object> payload;

        @BeforeEach
        void setUp() {
                payload = Map.of(
                                "title", "Integration Matrix",
                                "type", MediaType.MOVIE.name(),
                                "releaseYear", 1999);
        }

        @Test
        void full_crud_flow() throws Exception {
                // create
                var createResp = mvc.perform(post("/api/media")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(payload)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").exists())
                                .andExpect(jsonPath("$.title").value("Integration Matrix"))
                                .andReturn();

                var id = mapper.readTree(createResp.getResponse().getContentAsString()).get("id").asLong();

                // get
                mvc.perform(get("/api/media/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id));

                // list
                mvc.perform(get("/api/media").param("q", "Matrix"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Integration Matrix"));

                // update
                var upd = Map.of("title", "Integration Matrix Reloaded", "type", MediaType.MOVIE.name());
                mvc.perform(put("/api/media/{id}", id)
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(upd)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Integration Matrix Reloaded"));

                // delete
                mvc.perform(delete("/api/media/{id}", id))
                                .andExpect(status().isNoContent());
        }
}
