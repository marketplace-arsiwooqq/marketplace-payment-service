package com.arsiwooqq.paymentservice.exception;

import com.arsiwooqq.paymentservice.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.UnsatisfiedServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthorizationDeniedException(AccessDeniedException e) {
        log.debug("Access denied: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage())
                );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException e) {
        log.debug("Authentication failed: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Authentication failed")
                );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e) {
        log.debug("Method {} not allowed. Allowed methods: {}", e.getMethod(), e.getSupportedMethods());
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error("Method not allowed")
                );
    }

    @ExceptionHandler(UnsatisfiedServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsatisfiedParam(UnsatisfiedServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(
                ApiResponse.error("Missing or invalid request parameters: " + e.getMessage())
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(NoResourceFoundException e) {
        log.debug("Handler {} not found.", e.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Endpoint not found")
                );
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestParameter(MissingServletRequestParameterException e) {
        log.debug("Required parameter is missing: {}", e.getParameterName());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error("Parameter is missing: " + e.getParameterName()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneralException(Exception e) {
        log.error("Exception occurred: {}. Message: {}", e.getClass(), e.getMessage());
        log.debug("Exception:", e);
        return ResponseEntity
                .internalServerError()
                .body(("Internal server error")
                );
    }
}
