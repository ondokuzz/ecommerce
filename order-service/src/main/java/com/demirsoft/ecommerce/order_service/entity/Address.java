package com.demirsoft.ecommerce.order_service.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Address {
    private String state;
    private String city;
    private String street;
}