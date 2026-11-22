package com.practice.order.gateway;

public class PaymentGatewayException extends RuntimeException {

    private final boolean retryable;

    public PaymentGatewayException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public PaymentGatewayException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
