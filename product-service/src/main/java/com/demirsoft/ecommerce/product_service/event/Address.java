package com.demirsoft.ecommerce.product_service.event;

import lombok.Data;

@Data
public class Address {
    private String state;
    private String city;
    private String street;
}