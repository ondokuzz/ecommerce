package com.demirsoft.ecommerce.api_gateway.security;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.demirsoft.ecommerce.api_gateway.service.AuthService;

import io.jsonwebtoken.Claims;
import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

@Component
@RefreshScope
@Log4j2
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    @Autowired
    RouteValidator routeValidator;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AuthService authService;

    public static class Config {
    }

    public AuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> filter(exchange, chain);
    }

    private Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String token = "";
        ServerHttpRequest request = exchange.getRequest();
        log.info("filter {}", request);
        if (isValidRoute(request)) {
            log.info("validating authentication token");
            if (isCredsMissing(request)) {
                log.info("in error");
                return onError(exchange, "Credentials missing", HttpStatus.UNAUTHORIZED);
            }

            if (isLoginOrRegisterRequest(request)) {
                token = getTokenFromAuthService(request).orElse("invalid token");
            } else if (alreadyLoggedIn(request)) {
                token = extractTokenFromAuthorizationHeader(request).orElse("invalid token");
            }

            String secret = authService.getSecret();
            if (isTokenInvalid(token, secret)) {
                return this.onError(exchange, "Auth header invalid", HttpStatus.UNAUTHORIZED);
            } else {
                log.info("Authentication is successful");
            }

            try {
                this.populateRequestWithHeaders(exchange, token, secret);
            } catch (CertificateException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                log.error("populateRequestWithHeaders failed" + e.getMessage());
                return this.onError(exchange, "Auth header invalid", HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange);
    }

    private boolean isValidRoute(ServerHttpRequest request) {
        return routeValidator.isSecured.test(request);
    }

    private Optional<String> getTokenFromAuthService(ServerHttpRequest request) {
        List<String> usernameList = request.getHeaders().getOrEmpty("username");
        List<String> passwordList = request.getHeaders().getOrEmpty("password");
        if (usernameList.isEmpty())
            return Optional.empty();
        if (passwordList.isEmpty())
            return Optional.empty();

        String username = usernameList.get(usernameList.size() - 1);
        String password = passwordList.get(passwordList.size() - 1);

        if (username == null || username.isBlank())
            return Optional.empty();
        if (password == null || password.isBlank())
            return Optional.empty();

        return Optional.of(authService.getToken(username, password));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    private boolean isLoginOrRegisterRequest(ServerHttpRequest request) {
        return hasLoginInfo(request);
    }

    private boolean alreadyLoggedIn(ServerHttpRequest request) {
        return hasAuthorizationHeader(request);
    }

    private boolean hasAuthorizationHeader(ServerHttpRequest request) {
        return request.getHeaders().containsKey("Authorization");
    }

    private boolean isCredsMissing(ServerHttpRequest request) {
        return !hasCreds(request);
    }

    private boolean hasCreds(ServerHttpRequest request) {
        return hasLoginInfo(request) || hasAuthorizationHeader(request);
    }

    private boolean hasLoginInfo(ServerHttpRequest request) {
        return request.getHeaders().containsKey("username") && request.getHeaders().containsKey("password");
    }

    private Optional<String> extractTokenFromAuthorizationHeader(ServerHttpRequest request) {
        if (!request.getHeaders().containsKey("Authorization"))
            return Optional.empty();

        List<String> bearerAndTokenStrList = request.getHeaders().getOrEmpty("Authorization");
        bearerAndTokenStrList.stream().forEach(str -> log.info(str));
        if (bearerAndTokenStrList.isEmpty() || bearerAndTokenStrList.size() != 1)
            return Optional.empty();

        String bearerAndTokenStr = bearerAndTokenStrList.get(0);

        if (bearerAndTokenStr == null || bearerAndTokenStr.isEmpty())
            return Optional.empty();

        String[] bearerAndTokenArray = bearerAndTokenStr.split(" ");

        if (bearerAndTokenArray == null || bearerAndTokenArray.length != 2)
            return Optional.empty();

        String token = bearerAndTokenArray[1];

        return Optional.of(token);
    }

    private boolean isTokenInvalid(String token, String secret) {
        return jwtUtil.isInValid(token, secret);
    }

    private void populateRequestWithHeaders(ServerWebExchange exchange, String token, String secret)
            throws CertificateException, NoSuchAlgorithmException, InvalidKeySpecException {
        Claims claims = jwtUtil.getALlClaims(token, secret);
        exchange.getRequest()
                .mutate()
                .header("id", String.valueOf(claims.get("id")))
                .header("role", String.valueOf(claims.get("role")))
                .build();
    }

}