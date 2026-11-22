package com.practice.order.dto;

import com.practice.order.domain.Order;

import java.time.Instant;

public record OrderResponse(
        String orderId,
        String userId,
        String productName,
        long amount,
        String status,
        String paymentId,
        long version
) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getUserId(),
                order.getProductName(),
                order.getAmount().amount(),
                order.getStatus().name(),
                order.getPaymentId(),
                order.getVersion()
        );
    }
}
