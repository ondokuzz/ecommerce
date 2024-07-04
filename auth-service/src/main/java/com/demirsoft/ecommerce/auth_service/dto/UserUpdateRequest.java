package com.demirsoft.ecommerce.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @NotEmpty(message = "Must not be Empty or NULL")
    private final String username;

    @NotEmpty(message = "Must not be Empty or NULL")
    private final String password;

    @NotEmpty(message = "Must not be Empty or NULL")
    private final String role;

    @Email(message = "Please enter a valid email")
    private final String email;

    @NotNull(message = "Must not be NULL")
    private final Address address;

}
