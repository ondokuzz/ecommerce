package com.demirsoft.ecommerce.order_service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class OrderServiceControllerAdvice extends ResponseEntityExceptionHandler {

        @ExceptionHandler(CartAlreadyExistsException.class)
        public ResponseEntity<CustomErrorResponse> handleCartAlreadyExistsExceptionn(
                        CartAlreadyExistsException ex,
                        WebRequest request) {

                var customErrorResponse = new CustomErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getClass().getSimpleName(),
                                List.of(ex.getMessage()));

                return ResponseEntity.badRequest().body(customErrorResponse);
        }

        @ExceptionHandler(CartNotFoundException.class)
        public ResponseEntity<CustomErrorResponse> handleCartNotFoundException(
                        CartNotFoundException ex,
                        WebRequest request) {

                var customErrorResponse = new CustomErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getClass().getSimpleName(),
                                List.of(ex.getMessage()));

                return ResponseEntity.badRequest().body(customErrorResponse);
        }

        @ExceptionHandler(OrderEmptyException.class)
        public ResponseEntity<CustomErrorResponse> handleOrderEmptyException(
                        OrderEmptyException ex,
                        WebRequest request) {

                var customErrorResponse = new CustomErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getClass().getSimpleName(),
                                List.of(ex.getMessage()));

                return ResponseEntity.badRequest().body(customErrorResponse);
        }

        @ExceptionHandler(OrderNotFoundException.class)
        public ResponseEntity<CustomErrorResponse> handleOrderNotFoundException(
                        OrderNotFoundException ex,
                        WebRequest request) {

                var customErrorResponse = new CustomErrorResponse(
                                HttpStatus.BAD_REQUEST,
                                ex.getClass().getSimpleName(),
                                List.of(ex.getMessage()));

                return ResponseEntity.badRequest().body(customErrorResponse);
        }

}
