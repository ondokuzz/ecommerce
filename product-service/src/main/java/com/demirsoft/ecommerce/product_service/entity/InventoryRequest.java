package com.demirsoft.ecommerce.product_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;

@Document(collection = "inventory_request")
@Data
@AllArgsConstructor
public class InventoryRequest {
    @Id
    private String id;

    @Indexed
    private String orderId;

    private InventoryRequestStatus status;
}
