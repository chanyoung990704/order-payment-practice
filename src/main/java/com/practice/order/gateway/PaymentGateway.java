package com.practice.order.gateway;

import com.practice.order.domain.Money;

public interface PaymentGateway {

    ApprovalResult approve(String paymentKey, String orderId, Money amount);

    void cancel(String paymentKey, String cancelReason);

    record ApprovalResult(
            String paymentKey,
            String transactionKey,
            String status,
            long approvedAmount
    ) {
        public boolean isSuccess() {
            return "DONE".equalsIgnoreCase(status);
        }
    }
}
