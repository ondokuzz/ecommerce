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

        // @Autowired
        // AuthFilter authFilter;

        // @Bean
        // public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        // new org.springframework.cloud.gateway.route.RouteDefinition().

        // return builder.routes()
        // .route("auth-service", r -> r.path("/login")
        // .filters(f -> f.filters(authFilter))
        // .uri("http://localhost:8081"))
        // .route("auth-service", r -> r.path("/users")
        // .and().method("PUT")
        // .filters(f -> f.filters(authFilter))
        // .uri("http://localhost:8081"))
        // .route("auth-service", r -> r.path("/users")
        // .and().method("POST")
        // .uri("http://localhost:8081"))
        // .route("auth-service", r -> r.path("/public-key")
        // .uri("http://localhost:8081"))
        // .route("auth-service", r -> r.path("/.well-known/jwks.json")
        // .uri("http://localhost:8081"))
        // .route("product-service", r -> r.path("/product/**")
        // .uri("http://localhost:8082"))
        // .route("order-service", r -> r.path("/order/**")
        // .uri("http://localhost:8083"))
        // .route("auth-server", r -> r.path("/**")
        // .filters(f -> f.filters(authFilter))
        // .uri("http://localhost:8081"))
        // .build();
        // }

        @Bean
        public RestTemplate getRestTemplate() {
                return new RestTemplate();
        }

}