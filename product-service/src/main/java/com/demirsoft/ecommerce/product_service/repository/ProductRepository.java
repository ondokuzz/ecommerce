package com.demirsoft.ecommerce.product_service.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.demirsoft.ecommerce.product_service.entity.Product;

import reactor.core.publisher.Flux;

public interface ProductRepository extends ReactiveMongoRepository<Product, String> {
    Flux<Product> findByName(String name);
}
