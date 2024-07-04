package com.demirsoft.ecommerce.order_service.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "orders")
@Data
public class Order {
    @Id
    private String id;

    @Indexed
    private Long customerId;

    private List<OrderItem> items;

    private String status;

    private Double price;

    private String creditCardInfo;

    private Address shippingAddress;
}
