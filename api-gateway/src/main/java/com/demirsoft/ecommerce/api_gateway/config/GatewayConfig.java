package com.demirsoft.ecommerce.api_gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.demirsoft.ecommerce.api_gateway.security.AuthFilter;

@Configuration
public class GatewayConfig {

        @Autowired
        AuthFilter authFilter;

        @Bean
        public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
                return builder.routes()
                                .route("first-microservice", r -> r.path("/order")
                                                .and().method("GET")
                                                .filters(f -> f.filters(authFilter))
                                                .uri("http://localhost:8088"))
                                .route("second-microservice", r -> r.path("/second")
                                                .and().method("POST")
                                                .filters(f -> f.filters(authFilter))
                                                .uri("http://localhost:8082"))
                                .route("auth-server", r -> r.path("/login")
                                                .filters(f -> f.filters(authFilter))
                                                .uri("http://localhost:8088"))
                                .route("auth-server", r -> r.path("/register")
                                                .uri("http://localhost:8088"))
                                .route("auth-server", r -> r.path("/.well-known/jwks.json")
                                                .uri("http://localhost:8088"))
                                .route("auth-server", r -> r.path("/**")
                                                .filters(f -> f.filters(authFilter))
                                                .uri("http://localhost:8088"))
                                .build();
        }

        @Bean
        public RestTemplate getRestTemplate() {
                return new RestTemplate();
        }

}