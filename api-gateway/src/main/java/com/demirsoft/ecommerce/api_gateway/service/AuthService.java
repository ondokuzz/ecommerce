package com.demirsoft.ecommerce.api_gateway.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.demirsoft.ecommerce.api_gateway.dto.AuthenticationRequest;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;

    public String getSecret() {
        ResponseEntity<String> response = restTemplate.exchange(
                "http://localhost:8081/public-key",
                HttpMethod.GET,
                null,
                String.class);
        System.out.println("public key:" + response.getBody());
        return response.getBody();
    }

    public String getToken(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", username);
        headers.set("password", password);
        HttpEntity<AuthenticationRequest> request = new HttpEntity<AuthenticationRequest>(
                new AuthenticationRequest(username, password), headers);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8081/login", HttpMethod.POST, request,
                String.class);
        System.out.println("token:" + response.getBody());
        return response.getBody();
    }
}