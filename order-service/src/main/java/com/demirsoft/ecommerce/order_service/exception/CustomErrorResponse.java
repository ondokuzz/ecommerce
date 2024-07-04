package com.demirsoft.ecommerce.order_service.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;

public class CustomErrorResponse {

    private final ProblemDetail problemDetail;

    public CustomErrorResponse(HttpStatus status, String message, List<String> details) {
        String detailsStr = details.stream().reduce("", (result, str) -> result.concat(str).concat(","));

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
