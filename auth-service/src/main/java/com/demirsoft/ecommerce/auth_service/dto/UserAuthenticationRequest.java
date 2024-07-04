package com.demirsoft.ecommerce.auth_service.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserAuthenticationRequest {

    @NotEmpty(message = "Must not be Empty or NULL")
    private final String username;

    @NotEmpty(message = "Must not be Empty or NULL")
    private final String password;

}
