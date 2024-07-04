package com.demirsoft.ecommerce.product_service.event;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class ProductDto {
    @NotEmpty(message = "Must not be Empty or NULL")
    private String name;

    @NotEmpty(message = "Must not be Empty or NULL")
    private String brand;

    @NotEmpty(message = "Must not be Empty or NULL")
    private String description;

    @NotEmpty(message = "Must not be Empty or NULL")
    private Double price;

    private int quantity;
}
