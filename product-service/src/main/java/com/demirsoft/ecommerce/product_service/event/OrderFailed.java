package com.demirsoft.ecommerce.product_service.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderFailed {
    public enum ServiceType {
        ORDER_SERVICE,
        INVENTORY_SERVICE,
        PAYMENT_SERVICE,
        SHIPPING_SERVICE
    }

    private String id;

    private Long customerId;

    private String service;

    private List<String> reason;
}
