package com.demirsoft.ecommerce.api_gateway.dto;

import lombok.Data;

@Data
public class AuthenticationRequest {

    private final String username;
    private final String password;

}
