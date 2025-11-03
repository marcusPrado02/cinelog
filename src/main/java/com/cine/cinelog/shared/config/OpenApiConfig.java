package com.cine.cinelog.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

        @Bean
        public OpenAPI cinelogOpenAPI() {
                var info = new Info()
                                .title("CineLog API")
                                .description("Backend para registrar e recomendar filmes/séries")
                                .version("v1")
                                .license(new License().name("MIT").url("https://opensource.org/licenses/MIT"))
                                .contact(new Contact().name("Marcus Prado").email("silvamarcusprado@gmail.com"));

                var jwtScheme = new SecurityScheme()
                                .name("BearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT");

                return new OpenAPI()
                                .info(info)
                                .servers(List.of(
                                                new Server().url("http://localhost:8080").description("Dev")))
                                .components(new Components().addSecuritySchemes("BearerAuth", jwtScheme))
                                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"));
        }
}
