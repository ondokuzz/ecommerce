package com.demirsoft.ecommerce.auth_service.exception;

public class UserOperationNotPermittedException extends RuntimeException {
    public UserOperationNotPermittedException(String message) {
        super(message);
    }

}
