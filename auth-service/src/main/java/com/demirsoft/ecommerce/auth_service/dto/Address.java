package com.demirsoft.ecommerce.auth_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class Address {
    @NotNull(message = "Must not be NULL")
    private final String state;
    @NotNull(message = "Must not be NULL")
    private final String city;
    @NotNull(message = "Must not be NULL")
    private final String street;
}