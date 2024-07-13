package com.demirsoft.ecommerce.order_service.event;

import java.util.List;

import com.demirsoft.ecommerce.order_service.entity.Address;
import com.demirsoft.ecommerce.order_service.entity.OrderItem;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreated {
    private String id;

    private Long customerId;

    private List<OrderItem> items;

    private Double price;

    private String creditCardInfo;

    private Address shippingAddress;
}
