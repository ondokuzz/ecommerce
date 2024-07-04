package com.demirsoft.ecommerce.order_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.demirsoft.ecommerce.order_service.entity.Order;

public interface OrderRepository extends MongoRepository<Order, String> {
    List<Order> findByCustomerId(Long customerId);
}