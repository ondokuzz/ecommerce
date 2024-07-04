package com.demirsoft.ecommerce.auth_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.lang.NonNull;
import org.springframework.web.ErrorResponse;
import java.util.List;

public class CustomErrorResponse implements ErrorResponse {

    private final ProblemDetail problemDetail;

    public CustomErrorResponse(HttpStatus status, String message, List<String> details) {
        String detailsStr = details.stream().reduce("", (result, str) -> result.concat(str));

        this.problemDetail = ProblemDetail.forStatusAndDetail(status, message);
        this.problemDetail.setProperty("details", detailsStr);
    }

    @Override
    @NonNull
    public HttpStatus getStatusCode() {
        return HttpStatus.valueOf(problemDetail.getStatus());
    }

    @Override
    @NonNull
    public ProblemDetail getBody() {
        return problemDetail;
    }
}
