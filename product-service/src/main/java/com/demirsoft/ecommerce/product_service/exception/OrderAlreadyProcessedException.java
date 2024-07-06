package com.demirsoft.ecommerce.product_service.exception;

public class OrderAlreadyProcessedException extends Exception {

    public OrderAlreadyProcessedException(String message) {
        super(message);
    }

}
