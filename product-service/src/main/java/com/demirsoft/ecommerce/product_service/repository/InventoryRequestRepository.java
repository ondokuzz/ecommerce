package com.demirsoft.ecommerce.product_service.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.demirsoft.ecommerce.product_service.entity.InventoryUpdateLog;

import reactor.core.publisher.Flux;

public interface InventoryRequestRepository extends ReactiveMongoRepository<InventoryUpdateLog, String> {
    Flux<InventoryUpdateLog> findByOrderId(String name);
}
