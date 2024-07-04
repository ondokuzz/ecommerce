package com.demirsoft.ecommerce.product_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "products")
@Data
public class Product {
    @Id
    private String id;

    private String name;
    private String brand;
    private String description;
    private Double price;
    private int quantity;
}