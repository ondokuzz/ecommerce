package com.demirsoft.ecommerce.product_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InventoryAllocated {
    private String orderId;

    private Long customerId;

    private String creditCardInfo;

    private Double price;

    private Address shippingAddress;
}
