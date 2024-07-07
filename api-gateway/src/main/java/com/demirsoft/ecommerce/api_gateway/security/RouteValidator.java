package com.demirsoft.ecommerce.api_gateway.security;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.function.Predicate;

@Component
@Log4j2
public class RouteValidator {
    public static final List<String> unprotectedURLs = List.of(
            "/login", "/logout",
            "/order", "/product", "/swagger-ui", "/v3/api-docs");

    public Predicate<ServerHttpRequest> isSecured = request -> unprotectedURLs.stream()
            .noneMatch(uri -> {
                log.info("is secured {}", request.getURI().getPath());
                // request.getMethod().POST && request.getURI().getPath().contains("/users")
                return request.getURI().getPath().contains(uri);
            });
}