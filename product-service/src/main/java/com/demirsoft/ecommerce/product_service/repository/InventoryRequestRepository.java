package com.demirsoft.ecommerce.product_service.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.demirsoft.ecommerce.product_service.entity.InventoryRequest;

import reactor.core.publisher.Flux;

public interface InventoryRequestRepository extends ReactiveMongoRepository<InventoryRequest, String> {
    Flux<InventoryRequest> findByOrderId(String name);
}
