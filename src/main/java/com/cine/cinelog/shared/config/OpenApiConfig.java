package com.cine.cinelog.shared.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Classe de configuração Spring para gerenciamento de openapi.
 *
 * <p>
 * Define beans e configurações necessárias para o funcionamento
 * adequado da aplicação.
 * </p>
 *
 * @since 1.0
 */
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

    /**
     * Customiza a documentação OpenAPI para parâmetros do tipo Pageable.
     * Corrige o problema do Swagger UI que interpreta 'sort' como array ao invés de
     * string.
     */
    @Bean
    public OperationCustomizer swaggerPageableCustomizer() {
        return (operation, handlerMethod) -> {
            // Verifica se algum parâmetro é do tipo Pageable
            boolean hasPageable = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(param -> Pageable.class.isAssignableFrom(param.getParameterType()));

            if (hasPageable && operation.getParameters() != null) {
                // Remove todos os parâmetros 'sort' (podem vir múltiplos ou como array)
                List<Parameter> params = operation.getParameters().stream()
                        .filter(p -> !p.getName().toLowerCase().contains("sort"))
                        .collect(Collectors.toList());

                // Adiciona apenas UM parâmetro 'sort' customizado como string simples
                StringSchema sortSchema = new StringSchema();
                sortSchema.setExample("id,asc");
                sortSchema.setDefault("id");
                sortSchema.setPattern("^[a-zA-Z0-9_]+(,(asc|desc))?$");

                Parameter sortParam = new Parameter()
                        .name("sort")
                        .in("query")
                        .required(false)
                        .description(
                                "Campo de ordenação (ex: 'id', 'name' ou 'id,asc', 'name,desc'). Formato: propriedade ou propriedade,direção")
                        .schema(sortSchema);

                params.add(sortParam);
                operation.setParameters(params);
            }

            return operation;
        };
    }
}
