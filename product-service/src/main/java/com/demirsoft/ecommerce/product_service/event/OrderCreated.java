package com.demirsoft.ecommerce.product_service.event;

import java.util.List;

import lombok.Data;

@Data
public class OrderCreated {
    private String id;

    private Long customerId;

    private List<OrderItem> items;

    private Double price;

    private String creditCardInfo;

    private Address shippingAddress;
}
