package com.demirsoft.ecommerce.auth_service.exception;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class AuthControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(java.sql.SQLIntegrityConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleIntegrityException(
            SQLIntegrityConstraintViolationException ex,
            WebRequest request) {

        var errorResponse = new CustomErrorResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                List.of(String.valueOf(ex.getErrorCode())));

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /*
     * @ExceptionHandler(org.springframework.web.bind.
     * MethodArgumentNotValidException.class)
     * public ResponseEntity<ErrorResponse> handleValidationExceptions(
     * MethodArgumentNotValidException ex,
     * WebRequest request) {
     * 
     * var errors = new LinkedList<String>();
     * 
     * ex.getBindingResult().getAllErrors().forEach((error) -> {
     * String fieldName = ((FieldError) error).getField();
     * String errorMessage = error.getDefaultMessage();
     * errors.add(String.format("%s:%s", fieldName, errorMessage));
     * });
     * 
     * var errorResponse = new CustomErrorResponse(
     * HttpStatus.BAD_REQUEST,
     * ex.getMessage(),
     * errors);
     * 
     * return ResponseEntity.badRequest().body(errorResponse);
     * }
     */
}
