package com.demirsoft.ecommerce.order_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.demirsoft.ecommerce.order_service.entity.Cart;
import java.util.List;

public interface CartRepository extends MongoRepository<Cart, String> {
    List<Cart> findByCustomerId(Long customerId);

    void deleteByCustomerId(Long customerId);
}