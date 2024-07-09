package com.demirsoft.ecommerce.order_service.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;

public class CustomErrorResponse {

    private final ProblemDetail problemDetail;

    public CustomErrorResponse(HttpStatus status, String message, List<String> details) {
        String detailsStr = details.stream().collect(Collectors.joining(","));

        this.problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        this.problemDetail.setProperty("details", detailsStr);
    }

    @NonNull
    public HttpStatus getStatusCode() {
        return HttpStatus.valueOf(problemDetail.getStatus());
    }

    @NonNull
    public ProblemDetail getBody() {
        return problemDetail;
    }
}
