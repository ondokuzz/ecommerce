package com.demirsoft.ecommerce.auth_service.dto;

public class UserAuthenticationResponse {

    private final String jwt;

    public UserAuthenticationResponse(String jwt) {
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
}
