package com.demirsoft.ecommerce.order_service.entity;

import lombok.Data;

@Data
public class OrderItem {
    private String productId;
    private int quantity;
    private Double price;
}