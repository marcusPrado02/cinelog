package com.cine.cinelog.shared.config;

import org.junit.jupiter.api.Test;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiGroupsTest {

    @Test
    void shouldCreateV1Group() {
        OpenApiGroups config = new OpenApiGroups();
        GroupedOpenApi grouped = config.V1Group();

        assertNotNull(grouped, "GroupedOpenApi should not be null");
        assertEquals("v1", grouped.getGroup(), "group name should be 'v1'");

        String[] paths = grouped.getPathsToMatch().stream().toArray(String[]::new);
        assertNotNull(paths, "pathsToMatch should not be null");
        assertArrayEquals(new String[] { "/api/**" }, paths, "pathsToMatch should contain '/api/**'");
    }

    @Test
    void v1GroupMethodShouldBeAnnotatedWithBean() throws NoSuchMethodException {
        assertTrue(OpenApiGroups.class
                .getDeclaredMethod("V1Group")
                .isAnnotationPresent(Bean.class), "V1Group method should be annotated with @Bean");
    }
}