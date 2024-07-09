package com.demirsoft.ecommerce.auth_service.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    public static final String ADD_AUTH_HEADER_TO_SWAGGER_DOC = "requires-jwt-token";

    @Bean
    GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("add-user-id-header")
                .addOperationCustomizer((operation, httpMethod) -> {
                    boolean containsAddAuthHeaderTag = operation.getTags().stream()
                            .anyMatch(t -> t.equals(ADD_AUTH_HEADER_TO_SWAGGER_DOC));

                    if (containsAddAuthHeaderTag) {
                        operation.addParametersItem(
                                new HeaderParameter()
                                        .name("Authorization")
                                        .description("Access Token")
                                        .required(true));
                    }
                    return operation;
                })
                .build();
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
