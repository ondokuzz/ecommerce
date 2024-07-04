package com.demirsoft.ecommerce.product_service.event;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderFailed {
    private String id;

    private Long customerId;

    private List<String> reason;
}
