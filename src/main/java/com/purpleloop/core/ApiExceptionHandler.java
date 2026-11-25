package com.purpleloop.core;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException exception) {
        return problem(exception.getStatusCode(), exception.getReason());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ProblemDetail> handleMethodNotSupported() {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this endpoint.");
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        if (detail != null && !detail.isBlank()) {
            problem.setDetail(detail);
        }
        return ResponseEntity.status(status).body(problem);
    }
}
