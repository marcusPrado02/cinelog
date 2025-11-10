package com.cine.cinelog.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void cinelogOpenAPI_shouldContainExpectedInfoAndServer() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.cinelogOpenAPI();

        assertNotNull(openAPI);

        // Info assertions
        assertNotNull(openAPI.getInfo());
        assertEquals("CineLog API", openAPI.getInfo().getTitle());
        assertEquals("Backend para registrar e recomendar filmes/séries", openAPI.getInfo().getDescription());
        assertEquals("v1", openAPI.getInfo().getVersion());

        assertNotNull(openAPI.getInfo().getLicense());
        assertEquals("MIT", openAPI.getInfo().getLicense().getName());
        assertEquals("https://opensource.org/licenses/MIT", openAPI.getInfo().getLicense().getUrl());

        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("Marcus Prado", openAPI.getInfo().getContact().getName());
        assertEquals("silvamarcusprado@gmail.com", openAPI.getInfo().getContact().getEmail());

        // Server assertions
        assertNotNull(openAPI.getServers());
        assertFalse(openAPI.getServers().isEmpty());
        Server server = openAPI.getServers().get(0);
        assertEquals("http://localhost:8080", server.getUrl());
        assertEquals("Dev", server.getDescription());
    }

    @Test
    void cinelogOpenAPI_shouldConfigureJwtSecuritySchemeAndRequirement() {
        OpenApiConfig config = new OpenApiConfig();
        OpenAPI openAPI = config.cinelogOpenAPI();

        assertNotNull(openAPI.getComponents());
        Map<String, SecurityScheme> schemes = openAPI.getComponents().getSecuritySchemes();
        assertNotNull(schemes);
        assertTrue(schemes.containsKey("BearerAuth"));

        SecurityScheme jwtScheme = schemes.get("BearerAuth");
        assertNotNull(jwtScheme);
        assertEquals(SecurityScheme.Type.HTTP, jwtScheme.getType());
        assertEquals("bearer", jwtScheme.getScheme());
        assertEquals("JWT", jwtScheme.getBearerFormat());
        assertEquals("BearerAuth", jwtScheme.getName());

        // Security requirement should reference the scheme
        assertNotNull(openAPI.getSecurity());
        assertFalse(openAPI.getSecurity().isEmpty());
        boolean hasBearerRequirement = openAPI.getSecurity().stream()
                .anyMatch(req -> req.containsKey("BearerAuth"));
        assertTrue(hasBearerRequirement);
    }
}