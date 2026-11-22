package com.practice.order.gateway;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class GatewayRetry {

    private static final int MAX_ATTEMPTS = 3;
    private static final Duration BACKOFF = Duration.ofMillis(200);

    public <T> T execute(Supplier<T> operation) {
        PaymentGatewayException lastException = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return operation.get();
            } catch (PaymentGatewayException exception) {
                lastException = exception;
                if (!exception.isRetryable() || attempt == MAX_ATTEMPTS) {
                    throw exception;
                }
                sleep(attempt);
            }
        }
        throw lastException;
    }

    private void sleep(int attempt) {
        try {
            Thread.sleep(BACKOFF.multipliedBy(attempt).toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentGatewayException("재시도 중단", true);
        }
    }
}
