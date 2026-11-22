package com.practice.order.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<Map<String, String>> handleOrderException(OrderException exception) {
        return ResponseEntity
                .status(exception.getErrorCode().getStatus())
                .body(Map.of("code", exception.getErrorCode().name(),
                        "message", exception.getErrorCode().getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", ErrorCode.INVALID_AMOUNT.name(),
                        "message", exception.getMessage()));
    }
}
