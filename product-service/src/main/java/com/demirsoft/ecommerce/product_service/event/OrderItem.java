package com.demirsoft.ecommerce.product_service.event;

import lombok.Data;

@Data
public class OrderItem {
    private String productId;
    private int quantity;
    private Double price;
}